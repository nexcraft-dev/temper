package dev.nexcraft.temper.core;

/**
 * Indicates that a rate limiter rejected a call because its quota was exhausted.
 */
public final class RateLimitRejectedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a rate-limit rejection.
     *
     * @param message the rejection description, which may be {@code null}
     */
    public RateLimitRejectedException(final String message) {
        super(message);
    }
}
