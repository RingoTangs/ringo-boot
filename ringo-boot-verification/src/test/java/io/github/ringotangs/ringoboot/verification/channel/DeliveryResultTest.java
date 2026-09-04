package io.github.ringotangs.ringoboot.verification.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeliveryResultTest {

    @Test
    void createsValidatedDeliveryResults() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        assertEquals(expiresAt, new DeliveryResult.Accepted(expiresAt).expiresAt());
        assertEquals(expiresAt, new DeliveryResult.Uncertain(expiresAt).expiresAt());
        assertThrows(NullPointerException.class, () -> new DeliveryResult.Accepted(null));
        assertThrows(NullPointerException.class, () -> new DeliveryResult.Uncertain(null));
    }
}
