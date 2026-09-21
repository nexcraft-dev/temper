package dev.nexcraft.temper.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimiterTest {
    @Test
    void buildRejectsUnsetLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiter.builder().period(Duration.ofSeconds(1)).build());
    }

    @Test
    void buildRejectsZeroLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiter.builder().limit(0).period(Duration.ofSeconds(1)).build());
    }

    @Test
    void buildRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiter.builder().limit(-1).period(Duration.ofSeconds(1)).build());
    }

    @Test
    void buildRejectsUnsetPeriod() {
        assertThrows(NullPointerException.class,
                () -> RateLimiter.builder().limit(1).build());
    }

    @Test
    void buildRejectsExplicitNullPeriod() {
        assertThrows(NullPointerException.class,
                () -> RateLimiter.builder().limit(1).period(null).build());
    }

    @Test
    void buildRejectsZeroPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiter.builder().limit(1).period(Duration.ZERO).build());
    }

    @Test
    void buildRejectsNegativePeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> RateLimiter.builder().limit(1).period(Duration.ofSeconds(-1)).build());
    }
}
