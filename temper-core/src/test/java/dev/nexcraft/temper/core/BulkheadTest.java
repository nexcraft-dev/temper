package dev.nexcraft.temper.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class BulkheadTest {
    @Test
    void directExecutionRejectsWithoutRunningTask() {
        Bulkhead bulkhead = Bulkhead.builder().maxConcurrentCalls(1).build();
        AtomicBoolean invoked = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> bulkhead.execute(() -> {
            invoked.set(true);
            return null;
        }));
        assertFalse(invoked.get());
    }

    @Test
    void buildRejectsUnsetMaximumConcurrentCalls() {
        assertThrows(IllegalArgumentException.class, () -> Bulkhead.builder().build());
    }

    @Test
    void buildRejectsZeroMaximumConcurrentCalls() {
        assertThrows(IllegalArgumentException.class, () -> Bulkhead.builder()
                .maxConcurrentCalls(0)
                .build());
    }

    @Test
    void buildRejectsNegativeMaximumConcurrentCalls() {
        assertThrows(IllegalArgumentException.class, () -> Bulkhead.builder()
                .maxConcurrentCalls(-1)
                .build());
    }
}
