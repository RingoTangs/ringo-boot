package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GlobalIssueRateLimitRuleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void mapsDifferentVerificationKeysToTheSameBucket() {
        GlobalIssueRateLimitRule rule = new GlobalIssueRateLimitRule("application-minute", 100, Duration.ofMinutes(1));
        IssueContext login =
                IssueContext.of(new VerificationKey("account", "login", "user@example.com"), VerificationChannel.EMAIL);
        IssueContext payment =
                IssueContext.of(new VerificationKey("payment", "confirm", "+8613800000000"), VerificationChannel.SMS);

        assertEquals(IssueLimitBucket.of("global"), rule.bucket(login));
        assertEquals(rule.bucket(login), rule.bucket(payment));
    }

    @Test
    void sharesQuotaAcrossDifferentVerificationKeys() {
        GlobalIssueRateLimitRule rule = new GlobalIssueRateLimitRule("application-minute", 1, Duration.ofMinutes(1));
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(rule), new InMemoryIssueRateLimitStore());
        VerificationKey first = new VerificationKey("account", "login", "user@example.com");
        VerificationKey second = new VerificationKey("payment", "confirm", "+8613800000000");

        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                manager.acquire(IssueContext.of(first, VerificationChannel.EMAIL), NOW));
        assertInstanceOf(
                IssueLimitResult.Throttled.class,
                manager.acquire(IssueContext.of(second, VerificationChannel.SMS), NOW.plusSeconds(1)));
    }

    @Test
    void rejectsInvalidDefinitionsAndNullContext() {
        assertThrows(NullPointerException.class, () -> new GlobalIssueRateLimitRule(null, 1, Duration.ofMinutes(1)));
        assertThrows(NullPointerException.class, () -> new GlobalIssueRateLimitRule("application-minute", 1, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalIssueRateLimitRule("application_minute", 1, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalIssueRateLimitRule("application-minute", 0, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalIssueRateLimitRule("application-minute", 1, Duration.ZERO));

        GlobalIssueRateLimitRule rule = new GlobalIssueRateLimitRule("application-minute", 1, Duration.ofMinutes(1));
        assertThrows(NullPointerException.class, () -> rule.bucket(null));
    }
}
