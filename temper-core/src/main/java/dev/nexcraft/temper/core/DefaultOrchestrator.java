package dev.nexcraft.temper.core;

import java.util.Objects;
import java.util.concurrent.Callable;

/** Executes tasks through one immutable fault-tolerance chain. */
final class DefaultOrchestrator implements Orchestrator {
    private final FaultToleranceChain chain;

    /**
     * Creates an orchestrator for a built chain.
     *
     * @param chain the chain to execute through
     */
    DefaultOrchestrator(FaultToleranceChain chain) {
        this.chain = Objects.requireNonNull(chain, "chain");
    }

    @Override
    public <T> T execute(Callable<T> task) throws Exception {
        return chain.execute(task);
    }
}
