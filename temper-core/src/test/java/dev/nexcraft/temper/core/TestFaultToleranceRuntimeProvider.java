package dev.nexcraft.temper.core;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import dev.nexcraft.temper.core.spi.FaultToleranceRuntimeProvider;

/**
 * Test-only runtime provider that preserves observable bulkhead ordering.
 */
public final class TestFaultToleranceRuntimeProvider implements FaultToleranceRuntimeProvider {
    private static final AtomicInteger CREATION_COUNT = new AtomicInteger();

    /**
     * Resets the number of runtimes created by this provider.
     */
    static void resetCreationCount() {
        CREATION_COUNT.set(0);
    }

    /**
     * Returns the number of runtimes created by this provider.
     *
     * @return the runtime creation count
     */
    static int creationCount() {
        return CREATION_COUNT.get();
    }

    @Override
    public boolean supports(final FaultTolerance definition) {
        return Objects.requireNonNull(definition, "definition") instanceof Bulkhead;
    }

    @Override
    public FaultTolerance create(final FaultTolerance definition) {
        Bulkhead bulkhead = (Bulkhead) Objects.requireNonNull(definition, "definition");
        CREATION_COUNT.incrementAndGet();
        return new FaultTolerance() {
            @Override
            public <T> T execute(final Callable<T> task) throws Exception {
                Objects.requireNonNull(task, "task");
                Logger.getLogger(Bulkhead.class.getName()).info("Executing Bulkhead");
                return bulkhead.execute(task);
            }
        };
    }
}
