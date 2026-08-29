package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueRateLimitExceptionTest {

    @Test
    void providesACommonHierarchyForEveryRateLimitFailure() {
        IssueRateLimitExceededException exceeded = new IssueRateLimitExceededException(List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(30)),
                new IssueLimitViolation("ip-hour", Duration.ofMinutes(10))));
        MissingIssueRateLimitRuleException missing = new MissingIssueRateLimitRuleException();
        IssueRateLimitStoreException store = new IssueRateLimitStoreException("store unavailable");

        assertInstanceOf(IssueRateLimitException.class, exceeded);
        assertInstanceOf(IssueRateLimitException.class, missing);
        assertInstanceOf(IssueRateLimitException.class, store);
        assertInstanceOf(VerificationException.class, exceeded);
        assertEquals(Duration.ofMinutes(10), exceeded.retryAfter());
        assertEquals(
                List.of("subject-minute", "ip-hour"),
                exceeded.violations().stream().map(IssueLimitViolation::ruleId).toList());
    }

    @Test
    void rejectsInvalidExceededViolations() {
        assertThrows(NullPointerException.class, () -> new IssueRateLimitExceededException(null));
        assertThrows(IllegalArgumentException.class, () -> new IssueRateLimitExceededException(List.of()));
        IssueLimitViolation duplicate = new IssueLimitViolation("subject-minute", Duration.ZERO);
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueRateLimitExceededException(List.of(duplicate, duplicate)));
    }
}
