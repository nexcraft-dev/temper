package dev.nexcraft.temper.failsafe;

import java.util.Objects;
import java.util.concurrent.Callable;

import dev.nexcraft.temper.core.Bulkhead;
import dev.nexcraft.temper.core.BulkheadRejectedException;
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
        return new BulkheadRuntime(failsafeBulkhead);
    }

    private static final class BulkheadRuntime implements FaultTolerance {
        private final dev.failsafe.Bulkhead<Object> bulkhead;

        private BulkheadRuntime(dev.failsafe.Bulkhead<Object> bulkhead) {
            this.bulkhead = bulkhead;
        }

        @Override
        public <T> T execute(Callable<T> task) throws Exception {
            Objects.requireNonNull(task, "task");
            if (!bulkhead.tryAcquirePermit()) {
                throw new BulkheadRejectedException("Bulkhead concurrency limit reached");
            }
            try {
                return task.call();
            } finally {
                bulkhead.releasePermit();
            }
        }
    }
}
