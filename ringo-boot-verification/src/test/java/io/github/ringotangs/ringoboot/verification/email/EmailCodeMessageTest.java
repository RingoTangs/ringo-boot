package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailCodeMessageTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void exposesSemanticFieldsAndSafeStringRepresentation() {
        EmailCodeMessage message = message("account", "login", "user@example.com", "123456", EXPIRES_AT);

        assertEquals("account", message.namespace());
        assertEquals("login", message.purpose());
        assertEquals("user@example.com", message.email());
        assertEquals("123456", message.code());
        assertEquals(EXPIRES_AT, message.expiresAt());
        assertTrue(message.toString().contains("email=<redacted>"));
        assertTrue(message.toString().contains("code=<redacted>"));
        assertFalse(message.toString().contains("user@example.com"));
        assertFalse(message.toString().contains("123456"));
    }

    @Test
    void rejectsNullAndBlankFields() {
        assertThrows(
                NullPointerException.class, () -> message(null, "login", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> message("account", null, "user@example.com", "123456", EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> message("account", "login", null, "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> message("account", "login", "user@example.com", null, EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> message("account", "login", "user@example.com", "123456", null));
        assertThrows(
                IllegalArgumentException.class, () -> message(" ", "login", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class,
                () -> message("account", " ", "user@example.com", "123456", EXPIRES_AT));
        assertThrows(IllegalArgumentException.class, () -> message("account", "login", " ", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class, () -> message("account", "login", "user@example.com", " ", EXPIRES_AT));
    }

    private EmailCodeMessage message(String namespace, String purpose, String email, String code, Instant expiresAt) {
        return new EmailCodeMessage(namespace, purpose, email, code, expiresAt);
    }
}
