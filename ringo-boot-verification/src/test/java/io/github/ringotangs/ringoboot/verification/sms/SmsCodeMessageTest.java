package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SmsCodeMessageTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void exposesSemanticFieldsAndSafeStringRepresentation() {
        SmsCodeMessage message = message("account", "login", "+8613800000000", "123456", EXPIRES_AT);

        assertEquals("account", message.namespace());
        assertEquals("login", message.purpose());
        assertEquals("+8613800000000", message.phoneNumber());
        assertEquals("123456", message.code());
        assertEquals(EXPIRES_AT, message.expiresAt());
        assertTrue(message.toString().contains("phoneNumber=<redacted>"));
        assertTrue(message.toString().contains("code=<redacted>"));
        assertFalse(message.toString().contains("+8613800000000"));
        assertFalse(message.toString().contains("123456"));
    }

    @Test
    void rejectsNullAndBlankFields() {
        assertThrows(NullPointerException.class, () -> message(null, "login", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> message("account", null, "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> message("account", "login", null, "123456", EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> message("account", "login", "+8613800000000", null, EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> message("account", "login", "+8613800000000", "123456", null));
        assertThrows(
                IllegalArgumentException.class, () -> message(" ", "login", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class, () -> message("account", " ", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(IllegalArgumentException.class, () -> message("account", "login", " ", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class, () -> message("account", "login", "+8613800000000", " ", EXPIRES_AT));
    }

    private SmsCodeMessage message(
            String namespace, String purpose, String phoneNumber, String code, Instant expiresAt) {
        return new SmsCodeMessage(namespace, purpose, phoneNumber, code, expiresAt);
    }
}
