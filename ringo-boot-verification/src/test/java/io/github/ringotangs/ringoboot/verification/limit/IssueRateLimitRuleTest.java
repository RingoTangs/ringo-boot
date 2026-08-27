package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IssueRateLimitRuleTest {

    private static final IssueContext CONTEXT =
            IssueContext.of(new VerificationKey("account", "login", "user@example.com"), VerificationChannel.EMAIL);

    @Test
    void createsGlobalAndConditionalRules() {
        IssueRateLimitRule global = IssueRateLimitRule.of(
                "subject-minute", context -> IssueLimitBucket.of(context.key().subject()), 2, Duration.ofMinutes(1));
        IssueRateLimitRule conditional = IssueRateLimitRule.of(
                "registration-hour",
                context -> context.key().purpose().equals("registration"),
                context -> IssueLimitBucket.of(context.key().subject()),
                5,
                Duration.ofHours(1));

        assertTrue(global.matches(CONTEXT));
        assertEquals(IssueLimitBucket.of("user@example.com"), global.bucket(CONTEXT));
        assertFalse(conditional.matches(CONTEXT));
    }

    @Test
    void validatesSimpleRuleDefinition() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitRule.of(
                        "UPPER_CASE", context -> IssueLimitBucket.of("x"), 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitRule.of("valid-id", context -> IssueLimitBucket.of("x"), 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> IssueRateLimitRule.of("valid-id", context -> IssueLimitBucket.of("x"), 1, Duration.ZERO));
        assertThrows(
                NullPointerException.class,
                () -> IssueRateLimitRule.of(
                        "valid-id",
                        (java.util.function.Function<IssueContext, IssueLimitBucket>) null,
                        1,
                        Duration.ofMinutes(1)));
    }
}
