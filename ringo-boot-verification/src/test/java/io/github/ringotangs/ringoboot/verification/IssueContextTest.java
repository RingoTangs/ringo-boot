package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IssueContextTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();

    @Test
    void storesChannelAndApplicationDefinedAttributes() {
        IssueContext context = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY)
                .with("ip-address", "203.0.113.10")
                .with("device-id", "device-123");

        assertEquals(VerificationChannel.EMAIL, context.channel());
        assertEquals(POLICY, context.policy());
        assertEquals("203.0.113.10", context.attribute("ip-address").orElseThrow());
        assertEquals("device-123", context.attribute("device-id").orElseThrow());
        assertTrue(context.attribute("session-id").isEmpty());
    }

    @Test
    void defensivelyCopiesAndExtendsImmutably() {
        Map<String, String> source = new HashMap<>();
        source.put("ip-address", "203.0.113.10");
        IssueContext original = new IssueContext(KEY, VerificationChannel.EMAIL, POLICY, source);
        source.put("device-id", "changed");

        IssueContext extended = original.with("device-id", "device-123");

        assertTrue(original.attribute("device-id").isEmpty());
        assertEquals("device-123", extended.attribute("device-id").orElseThrow());
        assertEquals(POLICY, extended.policy());
        assertThrows(
                UnsupportedOperationException.class, () -> original.attributes().put("x", "y"));
    }

    @Test
    void hidesSensitiveValuesFromToString() {
        String text = IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY)
                .with("ip-address", "203.0.113.10")
                .toString();

        assertTrue(text.contains("account"));
        assertTrue(text.contains("login"));
        assertTrue(text.contains("email"));
        assertTrue(text.contains("ip-address"));
        assertFalse(text.contains("user@example.com"));
        assertFalse(text.contains("203.0.113.10"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> IssueContext.of(null, VerificationChannel.EMAIL, POLICY));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY, null, POLICY));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY, VerificationChannel.EMAIL, null));
        assertThrows(NullPointerException.class, () -> new IssueContext(KEY, VerificationChannel.EMAIL, POLICY, null));
        assertThrows(
                NullPointerException.class,
                () -> IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with(null, "value"));
        assertThrows(
                NullPointerException.class,
                () -> IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with("ip", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with(" ", "value"));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContext.of(KEY, VerificationChannel.EMAIL, POLICY).with("ip", " "));
    }
}
