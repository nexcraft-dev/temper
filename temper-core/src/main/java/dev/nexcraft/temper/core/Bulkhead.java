package dev.nexcraft.temper.core;

/**
 * Immutable vendor-neutral configuration for a bulkhead fault-tolerance capability.
 */
public final class Bulkhead implements FaultTolerance {
    private final int maxConcurrentCalls;

    private Bulkhead(int maxConcurrentCalls) {
        if (maxConcurrentCalls < 1) {
            throw new IllegalArgumentException("maxConcurrentCalls must be at least 1");
        }
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    /**
     * Creates a builder for a bulkhead configuration.
     *
     * @return a new bulkhead builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured maximum number of concurrent calls.
     *
     * @return the maximum number of concurrent calls
     */
    public int maxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    /**
     * Builds immutable bulkhead configurations.
     */
    public static final class Builder {
        private int maxConcurrentCalls;

        private Builder() {
        }

        /**
         * Sets the maximum number of concurrent calls.
         *
         * @param maxConcurrentCalls the maximum number of concurrent calls
         * @return this builder
         */
        public Builder maxConcurrentCalls(int maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        /**
         * Builds a bulkhead configuration.
         *
         * @return an immutable bulkhead configuration
         * @throws IllegalArgumentException if the maximum number of concurrent calls is less than one
         */
        public Bulkhead build() {
            return new Bulkhead(maxConcurrentCalls);
        }
    }
}
