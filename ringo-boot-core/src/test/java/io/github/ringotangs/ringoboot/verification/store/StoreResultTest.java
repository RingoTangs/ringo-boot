package io.github.ringotangs.ringoboot.verification.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StoreResultTest {

    @Test
    void createsStoredResult() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        StoreResult stored = new StoreResult(expiresAt);

        assertEquals(expiresAt, stored.expiresAt());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new StoreResult(null));
    }
}
