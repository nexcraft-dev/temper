package dev.nexcraft.temper.failsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import dev.nexcraft.temper.core.Bulkhead;
import dev.nexcraft.temper.core.FaultToleranceChain;
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
            assertTrue(overflow.getCause().getClass().getName().contains("Bulkhead"));
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
    void preservesBulkheadBeforeRateLimiterOrder() throws Exception {
        RateLimiter rateLimiter = RateLimiter.builder().limit(10).period(Duration.ofMinutes(1)).build();
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .next(rateLimiter)
                .build();
        assertDeclarationOrder(chain, List.of("Executing RateLimiter"));
    }

    @Test
    void preservesRateLimiterBeforeBulkheadOrder() throws Exception {
        RateLimiter rateLimiter = RateLimiter.builder().limit(10).period(Duration.ofMinutes(1)).build();
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(rateLimiter)
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .build();
        assertDeclarationOrder(chain, List.of("Executing RateLimiter", "Executing RateLimiter"));
    }

    private static void assertDeclarationOrder(FaultToleranceChain chain, List<String> expectedLogs) throws Exception {
        List<String> messages = new ArrayList<>();
        MessageCapture capture = new MessageCapture(messages);
        Logger logger = Logger.getLogger(RateLimiter.class.getName());
        logger.addHandler(capture);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<Void> first = pool.submit(() -> chain.execute(() -> {
                entered.countDown();
                release.await();
                return null;
            }));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Future<Void> overflow = pool.submit(() -> chain.execute(() -> null));
            assertThrows(ExecutionException.class, () -> overflow.get(2, TimeUnit.SECONDS));
            assertEquals(expectedLogs, messages);
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
            logger.removeHandler(capture);
        }
    }

    private static final class MessageCapture extends Handler {
        private final List<String> messages;

        private MessageCapture(List<String> messages) {
            this.messages = messages;
        }

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
