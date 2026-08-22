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
    void resolvesKeyAndAdditionalDimensions() {
        IssueContext context = IssueContext.of(KEY)
                .with(IssueLimitDimension.IP_ADDRESS, "203.0.113.10")
                .with(IssueLimitDimension.DEVICE_ID, "device-123")
                .with(IssueLimitDimension.SESSION_ID, "session-456");

        assertEquals("account", context.value(IssueLimitDimension.NAMESPACE).orElseThrow());
        assertEquals("login", context.value(IssueLimitDimension.PURPOSE).orElseThrow());
        assertEquals(
                "user@example.com", context.value(IssueLimitDimension.SUBJECT).orElseThrow());
        assertEquals(
                "203.0.113.10", context.value(IssueLimitDimension.IP_ADDRESS).orElseThrow());
        assertEquals("device-123", context.value(IssueLimitDimension.DEVICE_ID).orElseThrow());
        assertEquals(
                "session-456", context.value(IssueLimitDimension.SESSION_ID).orElseThrow());
        assertTrue(IssueContext.of(KEY).value(IssueLimitDimension.IP_ADDRESS).isEmpty());
    }

    @Test
    void isImmutableAndDoesNotExposeSensitiveValuesInToString() {
        Map<IssueLimitDimension, String> attributes = new HashMap<>();
        attributes.put(IssueLimitDimension.IP_ADDRESS, "203.0.113.10");
        IssueContext original = new IssueContext(KEY, attributes);
        attributes.clear();
        IssueContext extended = original.with(IssueLimitDimension.DEVICE_ID, "device-123");

        assertEquals(
                "203.0.113.10", original.value(IssueLimitDimension.IP_ADDRESS).orElseThrow());
        assertTrue(original.value(IssueLimitDimension.DEVICE_ID).isEmpty());
        assertEquals("device-123", extended.value(IssueLimitDimension.DEVICE_ID).orElseThrow());
        assertThrows(
                UnsupportedOperationException.class, () -> original.attributes().clear());
        assertFalse(original.toString().contains(KEY.subject()));
        assertFalse(original.toString().contains("203.0.113.10"));
    }

    @Test
    void rejectsInvalidAttributes() {
        assertThrows(NullPointerException.class, () -> IssueContext.of(null));
        assertThrows(NullPointerException.class, () -> new IssueContext(KEY, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueContext.of(KEY).with(IssueLimitDimension.NAMESPACE, "other"));
        assertThrows(
                IllegalArgumentException.class, () -> IssueContext.of(KEY).with(IssueLimitDimension.IP_ADDRESS, " "));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY).with(null, "value"));
        assertThrows(NullPointerException.class, () -> IssueContext.of(KEY).with(IssueLimitDimension.IP_ADDRESS, null));
    }
}
