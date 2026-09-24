package dev.nexcraft.temper.failsafe;

import java.util.Objects;
import java.util.concurrent.Callable;

import dev.nexcraft.temper.core.FaultTolerance;
import dev.nexcraft.temper.core.RateLimitRejectedException;
import dev.nexcraft.temper.core.RateLimiter;
import dev.nexcraft.temper.core.spi.FaultToleranceRuntimeProvider;

/**
 * Failsafe runtime provider for Temper rate-limiter definitions.
 */
public final class FailsafeRateLimiterRuntimeProvider implements FaultToleranceRuntimeProvider {
    /**
     * Creates a provider for Failsafe-backed rate-limiter runtimes.
     */
    public FailsafeRateLimiterRuntimeProvider() {
    }

    @Override
    public boolean supports(final FaultTolerance definition) {
        return definition instanceof RateLimiter;
    }

    @Override
    public FaultTolerance create(final FaultTolerance definition) {
        if (!(definition instanceof RateLimiter rateLimiter)) {
            throw new IllegalArgumentException("Unsupported fault-tolerance definition: "
                    + (definition == null ? "null" : definition.getClass().getName()));
        }
        dev.failsafe.RateLimiter<Object> failsafeRateLimiter = dev.failsafe.RateLimiter.<Object>burstyBuilder(
                rateLimiter.limit(), rateLimiter.period()).build();
        return new RateLimiterRuntime(failsafeRateLimiter);
    }

    private static final class RateLimiterRuntime implements FaultTolerance {
        private final dev.failsafe.RateLimiter<Object> rateLimiter;

        private RateLimiterRuntime(final dev.failsafe.RateLimiter<Object> rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        public <T> T execute(final Callable<T> task) throws Exception {
            Objects.requireNonNull(task, "task");
            if (!rateLimiter.tryAcquirePermit()) {
                throw new RateLimitRejectedException("Rate limit reached");
            }
            return task.call();
        }
    }
}
