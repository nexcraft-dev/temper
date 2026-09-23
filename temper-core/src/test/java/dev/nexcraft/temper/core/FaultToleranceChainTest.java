package dev.nexcraft.temper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class FaultToleranceChainTest {
    @Test
    void executesBulkheadRuntimeThenCallable() throws Exception {
        Bulkhead bulkhead = Bulkhead.builder().maxConcurrentCalls(1).build();
        List<String> messages = new ArrayList<>();
        LogCapture capture = new LogCapture(messages);
        Logger.getLogger(Bulkhead.class.getName()).addHandler(capture);
        Logger.getLogger(FaultToleranceChainTest.class.getName()).addHandler(capture);
        try {
            String result = new String("result");
            assertSame(result, FaultToleranceChain.builder().next(bulkhead).build()
                    .execute(() -> {
                        Logger.getLogger(FaultToleranceChainTest.class.getName()).info("Callable");
                        return result;
                    }));
        } finally {
            Logger.getLogger(Bulkhead.class.getName()).removeHandler(capture);
            Logger.getLogger(FaultToleranceChainTest.class.getName()).removeHandler(capture);
        }
        assertEquals(List.of("Executing Bulkhead", "Callable"), messages);
    }

    @Test
    void rejectsRateLimiterWithoutRuntimeProvider() {
        RateLimiter rateLimiter = RateLimiter.builder().limit(1).period(Duration.ofSeconds(1)).build();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> FaultToleranceChain.builder().next(rateLimiter).build());
        assertTrue(failure.getMessage().contains(RateLimiter.class.getName()));
    }

    @Test
    void propagatesCheckedExceptionIdentity() {
        Exception expected = new Exception("checked failure");
        Exception actual = assertThrows(Exception.class, () -> FaultToleranceChain.builder().build()
                .execute(() -> { throw expected; }));
        assertSame(expected, actual);
    }

    @Test
    void propagatesRuntimeExceptionIdentity() {
        IllegalStateException expected = new IllegalStateException("runtime failure");
        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> FaultToleranceChain.builder().build().execute(() -> { throw expected; }));
        assertSame(expected, actual);
    }

    @Test
    void executesEmptyChainTerminalOnce() throws Exception {
        AtomicInteger invocations = new AtomicInteger();
        String result = new String("result");

        assertSame(result, FaultToleranceChain.builder().build().execute(() -> {
            invocations.incrementAndGet();
            return result;
        }));

        assertEquals(1, invocations.get());
    }

    @Test
    void createsBulkheadRuntimeOncePerBuiltChainAndReusesIt() throws Exception {
        TestFaultToleranceRuntimeProvider.resetCreationCount();
        FaultToleranceChain chain = FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .build();

        assertEquals(1, TestFaultToleranceRuntimeProvider.creationCount());
        assertEquals("first", chain.execute(() -> "first"));
        assertEquals("second", chain.execute(() -> "second"));
        assertEquals(1, TestFaultToleranceRuntimeProvider.creationCount());
    }

    @Test
    void rejectsNullComponentsAndTasks() throws Exception {
        assertThrows(NullPointerException.class,
                () -> FaultToleranceChain.builder().next((Bulkhead) null));
        assertThrows(NullPointerException.class,
                () -> FaultToleranceChain.builder().next((RateLimiter) null));

        Bulkhead bulkhead = Bulkhead.builder().maxConcurrentCalls(1).build();
        RateLimiter rateLimiter = RateLimiter.builder().limit(1).period(Duration.ofSeconds(1)).build();
        FaultToleranceChain chain = FaultToleranceChain.builder().build();
        assertThrows(NullPointerException.class, () -> bulkhead.execute(null));
        assertThrows(NullPointerException.class, () -> rateLimiter.execute(null));
        assertThrows(NullPointerException.class, () -> chain.execute(null));
    }

    @Test
    void rejectsDuplicateComponents() {
        assertThrows(IllegalStateException.class, () -> FaultToleranceChain.builder()
                .next(Bulkhead.builder().maxConcurrentCalls(1).build())
                .next(Bulkhead.builder().maxConcurrentCalls(1).build()));
        assertThrows(IllegalStateException.class, () -> FaultToleranceChain.builder()
                .next(RateLimiter.builder().limit(1).period(Duration.ofSeconds(1)).build())
                .next(RateLimiter.builder().limit(1).period(Duration.ofSeconds(1)).build()));
    }

    private static final class LogCapture extends Handler {
        private final List<String> messages;

        private LogCapture(List<String> messages) {
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
