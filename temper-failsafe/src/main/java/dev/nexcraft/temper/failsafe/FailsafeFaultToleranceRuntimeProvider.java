package dev.nexcraft.temper.failsafe;

import java.util.Objects;
import java.util.concurrent.Callable;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.FailsafeExecutor;
import dev.nexcraft.temper.core.Bulkhead;
import dev.nexcraft.temper.core.FaultTolerance;
import dev.nexcraft.temper.core.spi.FaultToleranceRuntimeProvider;

/**
 * Failsafe runtime provider for Temper bulkhead definitions.
 */
public final class FailsafeFaultToleranceRuntimeProvider implements FaultToleranceRuntimeProvider {
    /**
     * Creates a provider for Failsafe-backed Temper runtimes.
     */
    public FailsafeFaultToleranceRuntimeProvider() {
    }

    @Override
    public boolean supports(final FaultTolerance definition) {
        return definition instanceof Bulkhead;
    }

    @Override
    public FaultTolerance create(final FaultTolerance definition) {
        if (!(definition instanceof Bulkhead bulkhead)) {
            throw new IllegalArgumentException("Unsupported fault-tolerance definition: "
                    + (definition == null ? "null" : definition.getClass().getName()));
        }
        dev.failsafe.Bulkhead<Object> failsafeBulkhead = dev.failsafe.Bulkhead.of(bulkhead.maxConcurrentCalls());
        FailsafeExecutor<Object> executor = Failsafe.with(failsafeBulkhead);
        return new BulkheadRuntime(executor);
    }

    private static final class BulkheadRuntime implements FaultTolerance {
        private final FailsafeExecutor<Object> executor;

        private BulkheadRuntime(FailsafeExecutor<Object> executor) {
            this.executor = executor;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(Callable<T> task) throws Exception {
            Objects.requireNonNull(task, "task");
            try {
                return (T) executor.get(task::call);
            } catch (FailsafeException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception checked && !(cause instanceof RuntimeException)) {
                    throw checked;
                }
                throw exception;
            }
        }
    }
}
