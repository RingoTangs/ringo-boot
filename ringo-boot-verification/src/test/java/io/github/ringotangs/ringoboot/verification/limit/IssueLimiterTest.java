package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IssueLimiterTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final IssueContext CONTEXT =
            IssueContext.of(KEY, VerificationChannel.EMAIL, VerificationPolicy.defaults());
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void permitAllExplicitlyAllowsRequestsAndValidatesInputs() {
        IssueLimiter limiter = IssueLimiter.permitAll();

        assertInstanceOf(IssueLimitResult.Allowed.class, limiter.acquire(CONTEXT, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(null, NOW));
        assertThrows(NullPointerException.class, () -> limiter.acquire(CONTEXT, null));
    }
}
