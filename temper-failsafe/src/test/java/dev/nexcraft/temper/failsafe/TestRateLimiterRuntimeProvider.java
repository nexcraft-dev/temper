package dev.nexcraft.temper.failsafe;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import dev.nexcraft.temper.core.FaultTolerance;
import dev.nexcraft.temper.core.RateLimiter;
import dev.nexcraft.temper.core.spi.FaultToleranceRuntimeProvider;

/**
 * Test-only rate-limiter runtime used to observe chain ordering.
 */
public final class TestRateLimiterRuntimeProvider implements FaultToleranceRuntimeProvider {
    /**
     * Creates a test-only runtime provider for rate limiters.
     */
    public TestRateLimiterRuntimeProvider() {
    }

    @Override
    public boolean supports(final FaultTolerance definition) {
        return definition instanceof RateLimiter;
    }

    @Override
    public FaultTolerance create(final FaultTolerance definition) {
        if (!(definition instanceof RateLimiter)) {
            throw new IllegalArgumentException("Unsupported definition: "
                    + (definition == null ? "null" : definition.getClass().getName()));
        }
        return new FaultTolerance() {
            @Override
            public <T> T execute(final Callable<T> task) throws Exception {
                Objects.requireNonNull(task, "task");
                Logger.getLogger(RateLimiter.class.getName()).info("Executing RateLimiter");
                return task.call();
            }
        };
    }
}
