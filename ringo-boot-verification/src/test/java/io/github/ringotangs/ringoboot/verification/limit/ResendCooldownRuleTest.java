package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResendCooldownRuleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final IssueContext LOGIN_EMAIL =
            context("account", "login", "user@example.com", VerificationChannel.EMAIL);

    @Test
    void describesCooldownQuotaAndMatchesExactBusinessScope() {
        ResendCooldownRule rule =
                new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60));

        assertEquals("resend-cooldown-7-account-5-login-email", rule.id());
        assertEquals(1, rule.maxIssues());
        assertEquals(Duration.ofSeconds(60), rule.window());
        assertEquals(IssueLimitBucket.of("account", "login", "email", "user@example.com"), rule.bucket(LOGIN_EMAIL));
        assertTrue(rule.matches(LOGIN_EMAIL));
        assertFalse(rule.matches(context("payment", "login", "user@example.com", VerificationChannel.EMAIL)));
        assertFalse(rule.matches(context("account", "register", "user@example.com", VerificationChannel.EMAIL)));
        assertFalse(rule.matches(context("account", "login", "user@example.com", VerificationChannel.SMS)));
    }

    @Test
    void isolatesCooldownByBusinessChannelAndSubject() {
        List<IssueRateLimitRule> rules = List.of(
                new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)),
                new ResendCooldownRule("account", "login", VerificationChannel.SMS, Duration.ofSeconds(60)),
                new ResendCooldownRule("account", "register", VerificationChannel.EMAIL, Duration.ofSeconds(60)),
                new ResendCooldownRule("payment", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)));
        IssueRateLimitManager manager = new IssueRateLimitManager(rules, new InMemoryIssueRateLimitStore());

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(LOGIN_EMAIL, NOW));
        IssueLimitResult.Throttled throttled =
                assertInstanceOf(IssueLimitResult.Throttled.class, manager.acquire(LOGIN_EMAIL, NOW.plusSeconds(1)));
        assertEquals(Duration.ofSeconds(59), throttled.retryAfter());
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                manager.acquire(context("account", "login", "other@example.com", VerificationChannel.EMAIL), NOW));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                manager.acquire(context("account", "login", "user@example.com", VerificationChannel.SMS), NOW));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                manager.acquire(context("account", "register", "user@example.com", VerificationChannel.EMAIL), NOW));
        assertInstanceOf(
                IssueLimitResult.Allowed.class,
                manager.acquire(context("payment", "login", "user@example.com", VerificationChannel.EMAIL), NOW));
    }

    @Test
    void rejectsInvalidDefinitionsAndNullContexts() {
        assertThrows(
                NullPointerException.class,
                () -> new ResendCooldownRule(null, "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)));
        assertThrows(
                NullPointerException.class,
                () -> new ResendCooldownRule("account", null, VerificationChannel.EMAIL, Duration.ofSeconds(60)));
        assertThrows(
                NullPointerException.class,
                () -> new ResendCooldownRule("account", "login", null, Duration.ofSeconds(60)));
        assertThrows(
                NullPointerException.class,
                () -> new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule(
                        "user_account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule(
                        "account", "reset_password", VerificationChannel.EMAIL, Duration.ofSeconds(60)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule(
                        "account", "login", VerificationChannel.of("voice"), Duration.ofSeconds(60)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(-1)));

        ResendCooldownRule rule =
                new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60));
        assertThrows(NullPointerException.class, () -> rule.matches(null));
        assertThrows(NullPointerException.class, () -> rule.bucket(null));
    }

    private static IssueContext context(String namespace, String purpose, String subject, VerificationChannel channel) {
        return IssueContext.of(new VerificationKey(namespace, purpose, subject), channel);
    }
}
