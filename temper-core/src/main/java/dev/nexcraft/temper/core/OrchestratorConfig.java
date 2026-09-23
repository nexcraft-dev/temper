package dev.nexcraft.temper.core;

import java.util.Objects;

/**
 * Immutable chain configuration used to create an {@link Orchestrator}.
 */
public final class OrchestratorConfig {
    private final FaultToleranceChain faultToleranceChain;

    /**
     * Creates a configuration with an empty fault-tolerance chain.
     */
    public OrchestratorConfig() {
        this(FaultToleranceChain.builder().build());
    }

    /**
     * Creates a configuration using a previously built chain.
     *
     * @param faultToleranceChain the chain to execute application tasks through
     * @throws NullPointerException if the chain is null
     */
    public OrchestratorConfig(FaultToleranceChain faultToleranceChain) {
        this.faultToleranceChain = Objects.requireNonNull(faultToleranceChain, "faultToleranceChain");
    }

    /**
     * Returns the chain bound to this configuration.
     *
     * @return the configured chain
     */
    FaultToleranceChain faultToleranceChain() {
        return faultToleranceChain;
    }
}
