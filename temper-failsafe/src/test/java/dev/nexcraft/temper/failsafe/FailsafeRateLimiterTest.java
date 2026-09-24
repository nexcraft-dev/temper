package dev.nexcraft.temper.failsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import dev.nexcraft.temper.core.Bulkhead;
import dev.nexcraft.temper.core.FaultTolerance;
import dev.nexcraft.temper.core.FaultToleranceChain;
import dev.nexcraft.temper.core.Orchestrator;
import dev.nexcraft.temper.core.OrchestratorConfig;
import dev.nexcraft.temper.core.RateLimitRejectedException;
import dev.nexcraft.temper.core.RateLimiter;

class FailsafeRateLimiterTest {
    @Test
    void concurrentCallsNeverExceedTheConfiguredQuota() throws Exception {
        FaultToleranceChain chain = chain(2, Duration.ofMinutes(1));
        AtomicInteger invoked = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Boolean>> attempts = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                attempts.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return chain.execute(() -> {
                            invoked.incrementAndGet();
                            return true;
                        });
                    } catch (RateLimitRejectedException rejected) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(2, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(2, accepted);
            assertEquals(2, invoked.get());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void enforcesQuotaAndRejectsImmediatelyWithoutInvokingRejectedTask() throws Exception {
        FaultToleranceChain chain = chain(1, Duration.ofMinutes(1));
        AtomicInteger invoked = new AtomicInteger();
        assertEquals("first", chain.execute(() -> {
            invoked.incrementAndGet();
            return "first";
        }));
        assertThrows(RateLimitRejectedException.class, () -> chain.execute(() -> {
            invoked.incrementAndGet();
            return "rejected";
        }));
        assertEquals(1, invoked.get());
    }

    @Test
    void oneChainSharesQuotaAndSeparateChainsHaveIndependentQuotas() throws Exception {
        FaultToleranceChain shared = chain(2, Duration.ofMinutes(1));
        assertEquals(1, shared.execute(() -> 1));
        assertEquals(2, shared.execute(() -> 2));
        assertThrows(RateLimitRejectedException.class, () -> shared.execute(() -> 3));

        FaultToleranceChain first = chain(1, Duration.ofMinutes(1));
        FaultToleranceChain second = chain(1, Duration.ofMinutes(1));
        assertEquals("first", first.execute(() -> "first"));
        assertEquals("second", second.execute(() -> "second"));
        assertThrows(RateLimitRejectedException.class, () -> first.execute(() -> "overflow"));
    }

    @Test
    void checkedFailureIsIdenticalAndStillConsumesQuota() throws Exception {
        FaultToleranceChain chain = chain(1, Duration.ofMinutes(1));
        Exception checked = new Exception("checked");
        Exception observed = assertThrows(Exception.class, () -> chain.execute(() -> {
            throw checked;
        }));
        assertSame(checked, observed);
        assertThrows(RateLimitRejectedException.class, () -> chain.execute(() -> "overflow"));
    }

    @Test
    void configuredOrchestratorExecutesThroughRateLimiter() throws Exception {
        FaultToleranceChain chain = chain(1, Duration.ofMinutes(1));
        Orchestrator orchestrator = Orchestrator.create(new OrchestratorConfig(chain));
        assertEquals("done", orchestrator.execute(() -> "done"));
        assertThrows(RateLimitRejectedException.class, () -> orchestrator.execute(() -> "overflow"));
    }

    @Test
    void renewsQuotaAfterTheConfiguredPeriod() throws Exception {
        FaultToleranceChain chain = chain(1, Duration.ofMillis(250));
        assertEquals("first", chain.execute(() -> "first"));
        assertThrows(RateLimitRejectedException.class, () -> chain.execute(() -> "early"));
        assertTrue(await(() -> executesSuccessfully(chain), Duration.ofSeconds(3)));
    }

    @Test
    void providerSupportsOnlyRateLimiterAndCreatesReusableRuntime() throws Exception {
        FailsafeRateLimiterRuntimeProvider provider = new FailsafeRateLimiterRuntimeProvider();
        RateLimiter definition = RateLimiter.builder().limit(2).period(Duration.ofMinutes(1)).build();
        assertTrue(provider.supports(definition));
        assertFalse(provider.supports(null));
        assertFalse(provider.supports(Bulkhead.builder().maxConcurrentCalls(1).build()));
        FaultTolerance runtime = provider.create(definition);
        assertEquals("ok", runtime.execute(() -> "ok"));
        assertThrows(IllegalArgumentException.class, () -> provider.create(null));
        assertThrows(IllegalArgumentException.class,
                () -> provider.create(Bulkhead.builder().maxConcurrentCalls(1).build()));
    }

    private static FaultToleranceChain chain(int limit, Duration period) {
        return FaultToleranceChain.builder()
                .next(RateLimiter.builder().limit(limit).period(period).build())
                .build();
    }

    private static boolean executesSuccessfully(FaultToleranceChain chain) {
        try {
            chain.execute(() -> "renewed");
            return true;
        } catch (RateLimitRejectedException rejected) {
            return false;
        } catch (Exception unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    private static boolean await(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.yield();
        }
        return condition.getAsBoolean();
    }
}
