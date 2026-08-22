package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IssueContextTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");

    @Test
    void storesApplicationDefinedAttributes() {
        IssueContext context =
                IssueContext.of(KEY).with("ip-address", "203.0.113.10").with("device-id", "device-123");

        assertEquals("203.0.113.10", context.attribute("ip-address").orElseThrow());
        assertEquals("device-123", context.attribute("device-id").orElseThrow());
        assertTrue(context.attribute("session-id").isEmpty());
    }

    @Test
    void defensivelyCopiesAndExtendsImmutably() {
        Map<String, String> source = new HashMap<>();
        source.put("ip-address", "203.0.113.10");
        IssueContext original = new IssueContext(KEY, source);
        source.put("device-id", "changed");

        IssueContext extended = original.with("device-id", "device-123");

        assertTrue(original.attribute("device-id").isEmpty());
        assertEquals("device-123", extended.attribute("device-id").orElseThrow());
        assertThrows(
                UnsupportedOperationException.class, () -> original.attributes().put("x", "y"));
    }

    @Test
    void hidesSensitiveValuesFromToString() {
        String text = IssueContext.of(KEY).with("ip-address", "203.0.113.10").toString();

        assertTrue(text.contains("account"));
        assertTrue(text.contains("login"));
        assertTrue(text.contains("ip-address"));
        assertFalse(text.contains("user@example.com"));
        assertFalse(text.contains("203.0.113.10"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> IssueContext.of(null));
        assertThrows(NullPointerException.class, () -> new IssueContext(KEY, null));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY).with(null, "value"));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY).with("ip", null));
        assertThrows(IllegalArgumentException.class, () -> IssueContext.of(KEY).with(" ", "value"));
        assertThrows(IllegalArgumentException.class, () -> IssueContext.of(KEY).with("ip", " "));
    }
}
