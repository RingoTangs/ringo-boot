package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueLimitExceptionTest {

    @Test
    void providesACommonHierarchyForEveryLimitFailure() {
        IssueLimitExceededException exceeded = new IssueLimitExceededException(List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(30)),
                new IssueLimitViolation("ip-hour", Duration.ofMinutes(10))));
        MissingIssueLimitRuleException missing = new MissingIssueLimitRuleException();
        IssueLimitStoreException store = new IssueLimitStoreException("store unavailable");

        assertInstanceOf(IssueLimitException.class, exceeded);
        assertInstanceOf(IssueLimitException.class, missing);
        assertInstanceOf(IssueLimitException.class, store);
        assertInstanceOf(VerificationException.class, exceeded);
        assertEquals(Duration.ofMinutes(10), exceeded.retryAfter());
        assertEquals(
                List.of("subject-minute", "ip-hour"),
                exceeded.violations().stream().map(IssueLimitViolation::ruleId).toList());
    }

    @Test
    void rejectsInvalidExceededViolations() {
        assertThrows(NullPointerException.class, () -> new IssueLimitExceededException(null));
        assertThrows(IllegalArgumentException.class, () -> new IssueLimitExceededException(List.of()));
        IssueLimitViolation duplicate = new IssueLimitViolation("subject-minute", Duration.ZERO);
        assertThrows(
                IllegalArgumentException.class, () -> new IssueLimitExceededException(List.of(duplicate, duplicate)));
    }
}
