package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueRateLimitPolicyTest {

    @Test
    void preservesOrderAndDefensivelyCopiesRules() {
        IssueRateLimitRule minute = rule(2, Duration.ofMinutes(1));
        IssueRateLimitRule hour = rule(10, Duration.ofHours(1));
        List<IssueRateLimitRule> source = new ArrayList<>(List.of(minute, hour));

        IssueRateLimitPolicy policy = new IssueRateLimitPolicy(source);
        source.clear();

        assertEquals(List.of(minute, hour), policy.rules());
        assertThrows(UnsupportedOperationException.class, () -> policy.rules().clear());
    }

    @Test
    void supportsAnExplicitEmptyPolicy() {
        assertTrue(IssueRateLimitPolicy.none().rules().isEmpty());
        assertTrue(IssueRateLimitPolicy.of().rules().isEmpty());
    }

    @Test
    void rejectsNullAndDuplicateRules() {
        IssueRateLimitRule rule = rule(2, Duration.ofMinutes(1));

        assertThrows(NullPointerException.class, () -> new IssueRateLimitPolicy(null));
        assertThrows(NullPointerException.class, () -> new IssueRateLimitPolicy(Arrays.asList(rule, null)));
        assertThrows(
                IllegalArgumentException.class, () -> IssueRateLimitPolicy.of(rule, rule(3, Duration.ofMinutes(1))));
    }

    private IssueRateLimitRule rule(int maxIssues, Duration window) {
        return new IssueRateLimitRule(IssueLimitScope.SUBJECT, maxIssues, window);
    }
}
