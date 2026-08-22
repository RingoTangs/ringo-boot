package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueRateLimitValidatorTest {

    @Test
    void acceptsValidRuleDefinition() {
        assertDoesNotThrow(
                () -> IssueRateLimitValidator.validateRuleDefinition("login-subject-minute", 2, Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNullFields() {
        assertThrows(
                NullPointerException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition(null, 1, Duration.ofMinutes(1)));
        assertThrows(
                NullPointerException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject-minute", 1, null));
    }

    @Test
    void rejectsInvalidRuleId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject_minute", 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("Subject-Minute", 1, Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNonPositiveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject-minute", 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject-minute", -1, Duration.ofMinutes(1)));
    }

    @Test
    void rejectsNonPositiveWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject-minute", 1, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitValidator.validateRuleDefinition("subject-minute", 1, Duration.ofSeconds(-1)));
    }
}
