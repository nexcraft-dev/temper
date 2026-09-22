package dev.nexcraft.temper.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Immutable vendor-neutral configuration for a rate-limiter fault-tolerance capability.
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
     * Executes a task through this rate-limiter configuration.
     *
     * @param task the task to execute
     * @param <T> the task result type
     * @return the task result
     * @throws Exception if task execution fails
     */
    @Override
    public <T> T execute(Callable<T> task) throws Exception {
        Objects.requireNonNull(task, "task");
        Logger.getLogger(RateLimiter.class.getName()).info("Executing RateLimiter");
        return task.call();
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
