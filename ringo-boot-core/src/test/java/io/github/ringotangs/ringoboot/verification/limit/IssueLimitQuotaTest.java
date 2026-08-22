package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueLimitQuotaTest {

    @Test
    void preservesResolvedQuota() {
        IssueLimitBucket bucket = IssueLimitBucket.of("account", "login", "user@example.com");

        IssueLimitQuota quota = new IssueLimitQuota("subject-minute", bucket, 2, Duration.ofMinutes(1));

        assertEquals("subject-minute", quota.ruleId());
        assertEquals(bucket, quota.bucket());
        assertEquals(2, quota.maxIssues());
        assertEquals(Duration.ofMinutes(1), quota.window());
    }

    @Test
    void rejectsNullFields() {
        IssueLimitBucket bucket = IssueLimitBucket.of("subject");

        assertThrows(NullPointerException.class, () -> new IssueLimitQuota(null, bucket, 1, Duration.ofMinutes(1)));
        assertThrows(
                NullPointerException.class,
                () -> new IssueLimitQuota("subject-minute", null, 1, Duration.ofMinutes(1)));
        assertThrows(NullPointerException.class, () -> new IssueLimitQuota("subject-minute", bucket, 1, null));
    }

    @Test
    void rejectsInvalidRuleDefinition() {
        IssueLimitBucket bucket = IssueLimitBucket.of("subject");

        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueLimitQuota("subject_minute", bucket, 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IssueLimitQuota("subject-minute", bucket, 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class, () -> new IssueLimitQuota("subject-minute", bucket, 1, Duration.ZERO));
    }
}
