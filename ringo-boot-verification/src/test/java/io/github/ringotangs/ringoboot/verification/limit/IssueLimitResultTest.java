package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueLimitResultTest {

    @Test
    void preservesOrderedViolationsAndCalculatesLargestRetryAfter() {
        var violations = new ArrayList<>(List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(30)),
                new IssueLimitViolation("ip-hour", Duration.ofMinutes(10))));

        IssueLimitResult.Throttled throttled = new IssueLimitResult.Throttled(violations);
        violations.clear();

        assertEquals(
                List.of("subject-minute", "ip-hour"),
                throttled.violations().stream().map(IssueLimitViolation::ruleId).toList());
        assertEquals(Duration.ofMinutes(10), throttled.retryAfter());
        assertThrows(
                UnsupportedOperationException.class,
                () -> throttled.violations().add(new IssueLimitViolation("other-rule", Duration.ZERO)));
    }

    @Test
    void rejectsInvalidViolations() {
        assertThrows(NullPointerException.class, () -> new IssueLimitResult.Throttled(null));
        assertThrows(IllegalArgumentException.class, () -> new IssueLimitResult.Throttled(List.of()));
        IssueLimitViolation duplicate = new IssueLimitViolation("subject-minute", Duration.ZERO);
        assertThrows(
                IllegalArgumentException.class, () -> new IssueLimitResult.Throttled(List.of(duplicate, duplicate)));
        assertThrows(IllegalArgumentException.class, () -> new IssueLimitViolation("invalid id", Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueLimitViolation("subject-minute", Duration.ofSeconds(-1)));
    }
}
