package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificationThrottledExceptionTest {

    @Test
    void preservesViolationsAndCalculatesLargestRetryAfter() {
        List<IssueLimitViolation> violations = List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(30)),
                new IssueLimitViolation("ip-hour", Duration.ofMinutes(10)));

        VerificationThrottledException exception = new VerificationThrottledException(violations);

        assertEquals(violations, exception.violations());
        assertEquals(Duration.ofMinutes(10), exception.retryAfter());
    }

    @Test
    void rejectsInvalidViolations() {
        assertThrows(NullPointerException.class, () -> new VerificationThrottledException(null));
        assertThrows(IllegalArgumentException.class, () -> new VerificationThrottledException(List.of()));
        IssueLimitViolation duplicate = new IssueLimitViolation("subject-minute", Duration.ZERO);
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationThrottledException(List.of(duplicate, duplicate)));
    }
}
