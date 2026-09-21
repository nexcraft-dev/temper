package dev.nexcraft.temper.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BulkheadTest {
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
