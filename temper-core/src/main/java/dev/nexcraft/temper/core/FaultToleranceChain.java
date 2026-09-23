package dev.nexcraft.temper.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

import dev.nexcraft.temper.core.spi.FaultToleranceRuntimeProvider;

/**
 * Immutable declaration-ordered composition of supported fault-tolerance components.
 */
public final class FaultToleranceChain implements FaultTolerance {
    private final Continuation head;

    private FaultToleranceChain(Continuation head) {
        this.head = head;
    }

    /**
     * Creates a builder for a fault-tolerance chain.
     *
     * @return a new chain builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Executes a task through each declared component and then the terminal task.
     *
     * @param task the task to execute
     * @param <T> the task result type
     * @return the task result
     * @throws Exception if task execution fails
     */
    @Override
    public <T> T execute(Callable<T> task) throws Exception {
        return head.execute(Objects.requireNonNull(task, "task"));
    }

    /**
     * Builds a fault-tolerance chain in declaration order.
     */
    public static final class Builder {
        private final List<FaultTolerance> components = new ArrayList<>();
        private boolean hasBulkhead;
        private boolean hasRateLimiter;

        private Builder() {
        }

        /**
         * Adds a bulkhead to the chain.
         *
         * @param bulkhead the bulkhead to add
         * @return this builder
         * @throws NullPointerException if bulkhead is null
         * @throws IllegalStateException if a bulkhead was already added
         */
        public Builder next(Bulkhead bulkhead) {
            Objects.requireNonNull(bulkhead, "bulkhead");
            if (hasBulkhead) {
                throw new IllegalStateException("Bulkhead already added");
            }
            hasBulkhead = true;
            components.add(bulkhead);
            return this;
        }

        /**
         * Adds a rate limiter to the chain.
         *
         * @param rateLimiter the rate limiter to add
         * @return this builder
         * @throws NullPointerException if rateLimiter is null
         * @throws IllegalStateException if a rate limiter was already added
         */
        public Builder next(RateLimiter rateLimiter) {
            Objects.requireNonNull(rateLimiter, "rateLimiter");
            if (hasRateLimiter) {
                throw new IllegalStateException("RateLimiter already added");
            }
            hasRateLimiter = true;
            components.add(rateLimiter);
            return this;
        }

        /**
         * Builds the immutable chain and composes its continuations once.
         *
         * @return an immutable fault-tolerance chain
         * @throws IllegalStateException if a component has no runtime provider,
         *         multiple providers, or a provider returns null
         */
        public FaultToleranceChain build() {
            Continuation composed = new TerminalContinuation();
            for (int index = components.size() - 1; index >= 0; index--) {
                composed = new ComponentContinuation(resolveRuntime(components.get(index)), composed);
            }
            return new FaultToleranceChain(composed);
        }

        private FaultTolerance resolveRuntime(final FaultTolerance component) {
            FaultToleranceRuntimeProvider supportingProvider = null;
            for (FaultToleranceRuntimeProvider provider : ServiceLoader.load(FaultToleranceRuntimeProvider.class)) {
                if (provider.supports(component)) {
                    if (supportingProvider != null) {
                        throw new IllegalStateException("Multiple runtime providers support "
                                + component.getClass().getName());
                    }
                    supportingProvider = provider;
                }
            }
            if (supportingProvider == null) {
                throw new IllegalStateException("No runtime provider supports "
                        + component.getClass().getName());
            }
            FaultTolerance runtime = supportingProvider.create(component);
            if (runtime == null) {
                throw new IllegalStateException("Runtime provider returned null for "
                        + component.getClass().getName());
            }
            return runtime;
        }
    }

    /**
     * Internal continuation contract for the pre-composed execution chain.
     */
    private interface Continuation {
        /**
         * Continues execution with the supplied task.
         *
         * @param task the task to execute
         * @param <T> the task result type
         * @return the task result
         * @throws Exception if task execution fails
         */
        <T> T execute(Callable<T> task) throws Exception;
    }

    /**
     * Internal continuation that delegates through one configured component.
     */
    private static final class ComponentContinuation implements Continuation {
        private final FaultTolerance component;
        private final Continuation next;

        private ComponentContinuation(FaultTolerance component, Continuation next) {
            this.component = component;
            this.next = next;
        }

        @Override
        public <T> T execute(Callable<T> task) throws Exception {
            return component.execute(() -> next.execute(task));
        }
    }

    /**
     * Internal terminal continuation that invokes the task exactly once.
     */
    private static final class TerminalContinuation implements Continuation {
        @Override
        public <T> T execute(Callable<T> task) throws Exception {
            return task.call();
        }
    }
}
