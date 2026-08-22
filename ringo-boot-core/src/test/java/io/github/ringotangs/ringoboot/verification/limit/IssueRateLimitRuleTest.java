package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IssueRateLimitRuleTest {

    @Test
    void exposesValidatedRuleValues() {
        Set<IssueLimitDimension> dimensions = new LinkedHashSet<>();
        dimensions.add(IssueLimitDimension.SESSION_ID);
        dimensions.add(IssueLimitDimension.SUBJECT);
        IssueRateLimitRule rule = new IssueRateLimitRule(dimensions, 10, Duration.ofHours(1));

        assertEquals(
                List.of(IssueLimitDimension.SUBJECT, IssueLimitDimension.SESSION_ID), List.copyOf(rule.dimensions()));
        assertEquals(10, rule.maxIssues());
        assertEquals(Duration.ofHours(1), rule.window());
        assertThrows(
                UnsupportedOperationException.class, () -> rule.dimensions().clear());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new IssueRateLimitRule(null, 1, Duration.ofMinutes(1)));
        assertThrows(
                NullPointerException.class, () -> new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 1, null));
        Set<IssueLimitDimension> nullDimension = new HashSet<>();
        nullDimension.add(null);
        assertThrows(NullPointerException.class, () -> new IssueRateLimitRule(nullDimension, 1, Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> new IssueRateLimitRule(Set.of(), 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 1, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitRule(Set.of(IssueLimitDimension.SUBJECT), 1, Duration.ofSeconds(-1)));
    }
}
