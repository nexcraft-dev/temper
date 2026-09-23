package dev.nexcraft.temper.core;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Public entry point for executing application tasks through a configured chain.
 */
public interface Orchestrator {
    /**
     * Creates an orchestrator using the supplied configuration.
     *
     * @param config the orchestrator configuration
     * @return a configured orchestrator
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public static Orchestrator create(OrchestratorConfig config) {
        Objects.requireNonNull(config, "config");
        return new DefaultOrchestrator(config.faultToleranceChain());
    }

    /**
     * Executes a task through the configured chain and returns its result without
     * wrapping task exceptions.
     *
     * @param task the task to execute
     * @param <T> the result type
     * @return the result returned by the task
     * @throws Exception if the task throws an exception
     * @throws BulkheadRejectedException if a configured bulkhead has no capacity
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public <T> T execute(Callable<T> task) throws Exception;
}
