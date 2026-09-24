package dev.nexcraft.temper.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Immutable vendor-neutral configuration for a rate-limiter fault-tolerance capability.
 *
 * <p>In a built chain, this definition allows at most {@link #limit()} calls
 * during each {@link #period()} fixed window. Calls near a window boundary can
 * use capacity from both adjacent windows in a short interval (a boundary burst).
 */
public final class RateLimiter implements FaultTolerance {
    private final int limit;
    private final Duration period;

    private RateLimiter(int limit, Duration period) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        this.period = Objects.requireNonNull(period, "period");
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period must be positive");
        }
        this.limit = limit;
    }

    /**
     * Creates a builder for a rate-limiter configuration.
     *
     * @return a new rate-limiter builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured request limit.
     *
     * @return the request limit
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns the configured rate-limiting period.
     *
     * @return the rate-limiting period
     */
    public Duration period() {
        return period;
    }

    /**
     * Rejects direct execution of this definition. Build a
     * {@link FaultToleranceChain} with a runtime provider to enforce it.
     *
     * @param task the task to execute
     * @param <T> the task result type
     * @return never returns normally
     * @throws IllegalStateException always, because this object is only a definition
     */
    @Override
    public <T> T execute(Callable<T> task) throws Exception {
        Objects.requireNonNull(task, "task");
        throw new IllegalStateException("RateLimiter requires a runtime provider");
    }

    /**
     * Builds immutable rate-limiter configurations.
     */
    public static final class Builder {
        private int limit;
        private Duration period;

        private Builder() {
        }

        /**
         * Sets the request limit.
         *
         * @param limit the request limit
         * @return this builder
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the rate-limiting period.
         *
         * @param period the rate-limiting period
         * @return this builder
         */
        public Builder period(Duration period) {
            this.period = period;
            return this;
        }

        /**
         * Builds a rate-limiter configuration.
         *
         * @return an immutable rate-limiter configuration
         * @throws IllegalArgumentException if the limit is less than one or the period is not positive
         * @throws NullPointerException if the period is null
         */
        public RateLimiter build() {
            return new RateLimiter(limit, period);
        }
    }
}
