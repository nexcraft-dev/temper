package dev.nexcraft.temper.core.spi;

import dev.nexcraft.temper.core.FaultTolerance;

/**
 * Provides reusable runtimes for vendor-neutral fault-tolerance definitions.
 */
public interface FaultToleranceRuntimeProvider {
    /**
     * Returns whether this provider supports the supplied definition.
     *
     * @param definition the non-null fault-tolerance definition
     * @return {@code true} if this provider can create a runtime for the definition
     */
    public abstract boolean supports(final FaultTolerance definition);

    /**
     * Creates a reusable runtime for the supplied definition.
     *
     * @param definition the non-null fault-tolerance definition
     * @return a non-null reusable runtime
     */
    public abstract FaultTolerance create(final FaultTolerance definition);
}
