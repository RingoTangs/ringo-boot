package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueLimitValidatorTest {

    @Test
    void acceptsValidRuleDefinition() {
        assertDoesNotThrow(
                () -> IssueLimitValidator.validateRuleDefinition("login-subject-minute", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> IssueLimitValidator.validateRuleDefinition(
                "rule@subject-quota:ns@account:purpose@login:channel@email:issues@2:window@1minutes",
                2,
                Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNullFields() {
        assertThrows(
                NullPointerException.class,
                () -> IssueLimitValidator.validateRuleDefinition(null, 1, Duration.ofMinutes(1)));
        assertThrows(
                NullPointerException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject-minute", 1, null));
    }

    @Test
    void rejectsInvalidRuleId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject_minute", 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("Subject-Minute", 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition(
                        "rule:subject-quota@ns:account@purpose:login@channel:email@issues:2@window:1minutes",
                        2,
                        Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition(
                        "rule:subject-quota:ns:account:purpose:login:channel:email:issues:2:window:1minutes",
                        2,
                        Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNonPositiveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject-minute", 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject-minute", -1, Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNonPositiveWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject-minute", 1, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueLimitValidator.validateRuleDefinition("subject-minute", 1, Duration.ofSeconds(-1)));
    }
}
