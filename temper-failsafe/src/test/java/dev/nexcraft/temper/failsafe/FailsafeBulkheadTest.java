package dev.nexcraft.temper.failsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.nexcraft.temper.core.Bulkhead;
import dev.nexcraft.temper.core.BulkheadRejectedException;
import dev.nexcraft.temper.core.FaultToleranceChain;
import dev.nexcraft.temper.core.Orchestrator;
import dev.nexcraft.temper.core.OrchestratorConfig;
import dev.nexcraft.temper.core.RateLimitRejectedException;
import dev.nexcraft.temper.core.RateLimiter;

class FailsafeBulkheadTest {
    @Test
    void limitsConcurrentEntriesAndRejectsOverflowImmediately() throws Exception {
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(2).build())
                .build();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Callable<String> blocked = () -> {
                int current = active.incrementAndGet();
                maximum.accumulateAndGet(current, Math::max);
                entered.countDown();
                release.await();
                active.decrementAndGet();
                return "ok";
            };
            Future<String> first = pool.submit(() -> chain.execute(blocked));
            Future<String> second = pool.submit(() -> chain.execute(blocked));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            ExecutionException overflow = assertThrows(ExecutionException.class,
                    () -> pool.submit(() -> chain.execute(blocked)).get());
            assertInstanceOf(BulkheadRejectedException.class, overflow.getCause());
            assertEquals(2, maximum.get());
            release.countDown();
            assertEquals("ok", first.get(2, TimeUnit.SECONDS));
            assertEquals("ok", second.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void releasesCapacityAfterNormalCompletionAndCheckedFailure() throws Exception {
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .build();
        assertEquals("first", chain.execute(() -> "first"));
        Exception checked = new Exception("checked");
        Exception observed = assertThrows(Exception.class, () -> chain.execute(() -> {
            throw checked;
        }));
        assertSame(checked, observed);
        assertEquals("after", chain.execute(() -> "after"));
    }

    @Test
    void configuredOrchestratorUsesTheBuiltBulkheadChain() throws Exception {
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .build();
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig(chain));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Void> first = pool.submit(() -> orchestrator.execute(() -> {
                entered.countDown();
                release.await();
                return null;
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertThrows(BulkheadRejectedException.class, () -> orchestrator.execute(() -> "overflow"));
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            assertEquals("after", orchestrator.execute(() -> "after"));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void bulkheadBeforeRateLimiterDoesNotSpendQuotaOnBulkheadRejection() throws Exception {
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .next(RateLimiter.builder().limit(2).period(Duration.ofMinutes(1)).build())
                .build();
        assertOrderingQuota(chain, false);
    }

    @Test
    void rateLimiterBeforeBulkheadSpendsQuotaOnBulkheadRejection() throws Exception {
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(RateLimiter.builder().limit(2).period(Duration.ofMinutes(1)).build())
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .build();
        assertOrderingQuota(chain, true);
    }

    private static void assertOrderingQuota(FaultToleranceChain chain, boolean overflowConsumesQuota) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Void> first = pool.submit(() -> chain.execute(() -> {
                entered.countDown();
                release.await();
                return null;
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertThrows(BulkheadRejectedException.class, () -> chain.execute(() -> null));
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            if (overflowConsumesQuota) {
                assertThrows(RateLimitRejectedException.class, () -> chain.execute(() -> null));
            } else {
                assertEquals("available", chain.execute(() -> "available"));
            }
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}
