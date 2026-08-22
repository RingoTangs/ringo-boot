package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailCodeDeliveryTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void exposesSemanticFieldsAndSafeStringRepresentation() {
        EmailCodeDelivery delivery = delivery("account", "login", "user@example.com", "123456", EXPIRES_AT);

        assertEquals("account", delivery.namespace());
        assertEquals("login", delivery.purpose());
        assertEquals("user@example.com", delivery.email());
        assertEquals("123456", delivery.code());
        assertEquals(EXPIRES_AT, delivery.expiresAt());
        assertTrue(delivery.toString().contains("email=<redacted>"));
        assertTrue(delivery.toString().contains("code=<redacted>"));
        assertFalse(delivery.toString().contains("user@example.com"));
        assertFalse(delivery.toString().contains("123456"));
    }

    @Test
    void rejectsNullAndBlankFields() {
        assertThrows(
                NullPointerException.class, () -> delivery(null, "login", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> delivery("account", null, "user@example.com", "123456", EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> delivery("account", "login", null, "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> delivery("account", "login", "user@example.com", null, EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> delivery("account", "login", "user@example.com", "123456", null));
        assertThrows(
                IllegalArgumentException.class, () -> delivery(" ", "login", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class,
                () -> delivery("account", " ", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(IllegalArgumentException.class, () -> delivery("account", "login", " ", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class,
                () -> delivery("account", "login", "user@example.com", " ", EXPIRES_AT));
    }

    private EmailCodeDelivery delivery(String namespace, String purpose, String email, String code, Instant expiresAt) {
        return new EmailCodeDelivery(namespace, purpose, email, code, expiresAt);
    }
}
