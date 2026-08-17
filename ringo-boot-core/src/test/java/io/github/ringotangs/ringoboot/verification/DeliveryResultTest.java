package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeliveryResultTest {

    @Test
    void createsSafeDeliveryResults() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        DeliveryResult.Accepted accepted = new DeliveryResult.Accepted(expiresAt);
        DeliveryResult.Uncertain uncertain = new DeliveryResult.Uncertain(expiresAt);
        DeliveryResult.Throttled throttled = new DeliveryResult.Throttled(Duration.ofSeconds(30));

        assertEquals(expiresAt, accepted.expiresAt());
        assertEquals(expiresAt, uncertain.expiresAt());
        assertEquals(Duration.ofSeconds(30), throttled.retryAfter());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new DeliveryResult.Accepted(null));
        assertThrows(NullPointerException.class, () -> new DeliveryResult.Uncertain(null));
        assertThrows(NullPointerException.class, () -> new DeliveryResult.Throttled(null));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryResult.Throttled(Duration.ofSeconds(-1)));
    }
}
