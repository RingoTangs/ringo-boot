package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SmsCodeDeliveryTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void exposesSemanticFieldsAndSafeStringRepresentation() {
        SmsCodeDelivery delivery = delivery("account", "login", "+8613800000000", "123456", EXPIRES_AT);

        assertEquals("account", delivery.namespace());
        assertEquals("login", delivery.purpose());
        assertEquals("+8613800000000", delivery.phoneNumber());
        assertEquals("123456", delivery.code());
        assertEquals(EXPIRES_AT, delivery.expiresAt());
        assertTrue(delivery.toString().contains("phoneNumber=<redacted>"));
        assertTrue(delivery.toString().contains("code=<redacted>"));
        assertFalse(delivery.toString().contains("+8613800000000"));
        assertFalse(delivery.toString().contains("123456"));
    }

    @Test
    void rejectsNullAndBlankFields() {
        assertThrows(NullPointerException.class, () -> delivery(null, "login", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> delivery("account", null, "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> delivery("account", "login", null, "123456", EXPIRES_AT));
        assertThrows(
                NullPointerException.class, () -> delivery("account", "login", "+8613800000000", null, EXPIRES_AT));
        assertThrows(NullPointerException.class, () -> delivery("account", "login", "+8613800000000", "123456", null));
        assertThrows(
                IllegalArgumentException.class, () -> delivery(" ", "login", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class, () -> delivery("account", " ", "+8613800000000", "123456", EXPIRES_AT));
        assertThrows(IllegalArgumentException.class, () -> delivery("account", "login", " ", "123456", EXPIRES_AT));
        assertThrows(
                IllegalArgumentException.class, () -> delivery("account", "login", "+8613800000000", " ", EXPIRES_AT));
    }

    private SmsCodeDelivery delivery(
            String namespace, String purpose, String phoneNumber, String code, Instant expiresAt) {
        return new SmsCodeDelivery(namespace, purpose, phoneNumber, code, expiresAt);
    }
}
