package io.github.ringotangs.ringoboot.verification.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StoreResultTest {

    @Test
    void createsStoredResult() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        StoreResult.Stored stored = new StoreResult.Stored(expiresAt);

        assertEquals(expiresAt, stored.expiresAt());
    }

    @Test
    void createsThrottledResult() {
        Duration retryAfter = Duration.ofSeconds(30);

        StoreResult.Throttled throttled = new StoreResult.Throttled(retryAfter);

        assertEquals(retryAfter, throttled.retryAfter());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new StoreResult.Stored(null));
        assertThrows(NullPointerException.class, () -> new StoreResult.Throttled(null));
        assertThrows(IllegalArgumentException.class, () -> new StoreResult.Throttled(Duration.ofSeconds(-1)));
    }
}
