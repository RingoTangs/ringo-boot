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
        ResendCooldownRule rule = rule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60));

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
                rule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)),
                rule("account", "login", VerificationChannel.SMS, Duration.ofSeconds(60)),
                rule("account", "register", VerificationChannel.EMAIL, Duration.ofSeconds(60)),
                rule("payment", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60)));
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
    void allowsAnotherIssueWhenTheCompleteCooldownHasElapsed() {
        ResendCooldownRule rule = rule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60));
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(rule), new InMemoryIssueRateLimitStore());

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(LOGIN_EMAIL, NOW));
        assertInstanceOf(IssueLimitResult.Throttled.class, manager.acquire(LOGIN_EMAIL, NOW.plusSeconds(59)));
        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(LOGIN_EMAIL, NOW.plusSeconds(60)));
    }

    @Test
    void supportsCustomVerificationChannels() {
        VerificationChannel imageCode = VerificationChannel.of("image-code");
        IssueContext context = context("security", "challenge", "challenge-123", imageCode);
        ResendCooldownRule rule = rule("security", "challenge", imageCode, Duration.ofSeconds(10));

        assertEquals("resend-cooldown-8-security-9-challenge-image-code", rule.id());
        assertTrue(rule.matches(context));
        assertEquals(IssueLimitBucket.of("security", "challenge", "image-code", "challenge-123"), rule.bucket(context));
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
                () -> new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(-1)));

        ResendCooldownRule rule = rule("account", "login", VerificationChannel.EMAIL, Duration.ofSeconds(60));
        assertThrows(NullPointerException.class, () -> rule.matches(null));
        assertThrows(NullPointerException.class, () -> rule.bucket(null));
    }

    @Test
    void builderCreatesTheSameRuleAsTheCanonicalConstructor() {
        ResendCooldownRule constructed =
                new ResendCooldownRule("account", "login", VerificationChannel.EMAIL, Duration.ofMinutes(1));

        assertEquals(constructed, rule("account", "login", VerificationChannel.EMAIL, Duration.ofMinutes(1)));
    }

    @Test
    void builderRejectsMissingAndInvalidFields() {
        assertThrows(
                NullPointerException.class,
                () -> ResendCooldownRule.builder()
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .cooldown(Duration.ofMinutes(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> ResendCooldownRule.builder()
                        .namespace("account")
                        .channel(VerificationChannel.EMAIL)
                        .cooldown(Duration.ofMinutes(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> ResendCooldownRule.builder()
                        .namespace("account")
                        .purpose("login")
                        .cooldown(Duration.ofMinutes(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> ResendCooldownRule.builder()
                        .namespace("account")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .build());
        assertThrows(
                NullPointerException.class, () -> ResendCooldownRule.builder().namespace(null));
        assertThrows(
                NullPointerException.class, () -> ResendCooldownRule.builder().purpose(null));
        assertThrows(
                NullPointerException.class, () -> ResendCooldownRule.builder().channel(null));
        assertThrows(
                NullPointerException.class, () -> ResendCooldownRule.builder().cooldown(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> rule("user_account", "login", VerificationChannel.EMAIL, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> rule("account", "reset_password", VerificationChannel.EMAIL, Duration.ofMinutes(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> rule("account", "login", VerificationChannel.EMAIL, Duration.ZERO));
    }

    private static ResendCooldownRule rule(
            String namespace, String purpose, VerificationChannel channel, Duration cooldown) {
        return ResendCooldownRule.builder()
                .namespace(namespace)
                .purpose(purpose)
                .channel(channel)
                .cooldown(cooldown)
                .build();
    }

    private static IssueContext context(String namespace, String purpose, String subject, VerificationChannel channel) {
        return IssueContext.of(new VerificationKey(namespace, purpose, subject), channel);
    }
}
