package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueRateLimitRuleTest {

    @Test
    void exposesValidatedRuleValues() {
        IssueRateLimitRule rule = new IssueRateLimitRule(IssueLimitScope.SUBJECT, 10, Duration.ofHours(1));

        assertEquals(IssueLimitScope.SUBJECT, rule.scope());
        assertEquals(10, rule.maxIssues());
        assertEquals(Duration.ofHours(1), rule.window());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new IssueRateLimitRule(null, 1, Duration.ofMinutes(1)));
        assertThrows(NullPointerException.class, () -> new IssueRateLimitRule(IssueLimitScope.SUBJECT, 1, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(IssueLimitScope.SUBJECT, 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(IssueLimitScope.SUBJECT, 1, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(IssueLimitScope.SUBJECT, 1, Duration.ofSeconds(-1)));
    }
}
