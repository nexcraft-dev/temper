package dev.nexcraft.temper.core;

/**
 * Indicates that a bulkhead rejected a call because no capacity was available.
 */
public final class BulkheadRejectedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a bulkhead rejection.
     *
     * @param message the rejection description
     */
    public BulkheadRejectedException(String message) {
        super(message);
    }
}
