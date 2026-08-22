package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IssueRateLimiterTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void permitAllExplicitlyAllowsRequestsAndValidatesInputs() {
        IssueRateLimiter limiter = IssueRateLimiter.permitAll();

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(KEY, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(null, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(KEY, null));
    }
}
