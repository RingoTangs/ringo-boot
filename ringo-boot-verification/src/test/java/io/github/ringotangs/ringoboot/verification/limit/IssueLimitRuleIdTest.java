package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueLimitRuleIdTest {

    @Test
    void formatsEverySupportedWindowUnitCanonically() {
        assertWindow(Duration.ofHours(48), "2days");
        assertWindow(Duration.ofMinutes(120), "2hours");
        assertWindow(Duration.ofSeconds(60), "1minutes");
        assertWindow(Duration.ofSeconds(90), "90seconds");
        assertWindow(Duration.ofMillis(1_500), "1500milliseconds");
        assertWindow(Duration.ofNanos(1_500_000_001L), "1500000001nanoseconds");
    }

    @Test
    void equivalentDurationsProduceTheSameId() {
        assertEquals(subjectId(Duration.ofSeconds(60)), subjectId(Duration.ofMinutes(1)));
    }

    @Test
    void supportsTheFullPositiveDurationRangeWithoutOverflow() {
        String id = subjectId(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L));

        assertTrue(id.endsWith("nanoseconds"));
        assertDoesNotThrow(() -> IssueLimitRuleId.validate("rule id", id));
    }

    @Test
    void ruleTypeAndEveryDefinitionParameterAffectTheId() {
        String baseline = subjectId(Duration.ofMinutes(1));

        assertNotEquals(
                baseline,
                IssueLimitRuleId.generate(
                        "purpose-quota", "account", "login", VerificationChannel.EMAIL, 5, Duration.ofMinutes(1)));
        assertNotEquals(
                baseline,
                IssueLimitRuleId.generate(
                        "subject-quota", "profile", "login", VerificationChannel.EMAIL, 5, Duration.ofMinutes(1)));
        assertNotEquals(
                baseline,
                IssueLimitRuleId.generate(
                        "subject-quota", "account", "register", VerificationChannel.EMAIL, 5, Duration.ofMinutes(1)));
        assertNotEquals(
                baseline,
                IssueLimitRuleId.generate(
                        "subject-quota", "account", "login", VerificationChannel.SMS, 5, Duration.ofMinutes(1)));
        assertNotEquals(
                baseline,
                IssueLimitRuleId.generate(
                        "subject-quota", "account", "login", VerificationChannel.EMAIL, 6, Duration.ofMinutes(1)));
        assertNotEquals(baseline, subjectId(Duration.ofMinutes(2)));
    }

    private static void assertWindow(Duration window, String expected) {
        assertTrue(subjectId(window).endsWith(":window@" + expected));
    }

    private static String subjectId(Duration window) {
        return IssueLimitRuleId.generate("subject-quota", "account", "login", VerificationChannel.EMAIL, 5, window);
    }
}
