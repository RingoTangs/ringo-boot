package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IssueResultTest {

    @Test
    void createsSafeIssueResults() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        IssueResult.Accepted accepted = new IssueResult.Accepted(expiresAt);
        IssueResult.Uncertain uncertain = new IssueResult.Uncertain(expiresAt);
        IssueResult.Throttled throttled = new IssueResult.Throttled(Duration.ofSeconds(30));

        assertEquals(expiresAt, accepted.expiresAt());
        assertEquals(expiresAt, uncertain.expiresAt());
        assertEquals(Duration.ofSeconds(30), throttled.retryAfter());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new IssueResult.Accepted(null));
        assertThrows(NullPointerException.class, () -> new IssueResult.Uncertain(null));
        assertThrows(NullPointerException.class, () -> new IssueResult.Throttled(null));
        assertThrows(IllegalArgumentException.class, () -> new IssueResult.Throttled(Duration.ofSeconds(-1)));
    }
}
