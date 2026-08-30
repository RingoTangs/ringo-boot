package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BuiltInQuotaRuleTest {

    private static final IssueContext LOGIN_EMAIL = context("account", "login", "user@example.com", "email");

    @Test
    void namespaceRuleAggregatesPurposesAndSubjectsWithinOneChannel() {
        NamespaceQuotaRule rule = NamespaceQuotaRule.builder()
                .id("account-email-hour")
                .namespace("account")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(100)
                .window(Duration.ofHours(1))
                .build();

        assertEquals(IssueLimitBucket.of("account", "email"), rule.bucket(LOGIN_EMAIL));
        assertTrue(rule.appliesTo(context("account", "register", "other@example.com", "email")));
        assertFalse(rule.appliesTo(context("account", "login", "13800138000", "sms")));
        assertFalse(rule.appliesTo(context("payment", "login", "user@example.com", "email")));
    }

    @Test
    void purposeRuleAggregatesSubjectsWithinOneChannel() {
        PurposeQuotaRule rule = PurposeQuotaRule.builder()
                .id("login-email-minute")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(20)
                .window(Duration.ofMinutes(1))
                .build();

        assertEquals(IssueLimitBucket.of("account", "login", "email"), rule.bucket(LOGIN_EMAIL));
        assertTrue(rule.appliesTo(context("account", "login", "other@example.com", "email")));
        assertFalse(rule.appliesTo(context("account", "login", "13800138000", "sms")));
        assertFalse(rule.appliesTo(context("account", "register", "user@example.com", "email")));
        assertFalse(rule.appliesTo(context("payment", "login", "user@example.com", "email")));
    }

    @Test
    void subjectRuleSeparatesChannelsAndSubjects() {
        SubjectQuotaRule rule = subjectRule("login-email-subject-hour", 5, Duration.ofHours(1));
        IssueContext sms = context("account", "login", "user@example.com", "sms");
        IssueContext anotherSubject = context("account", "login", "other@example.com", "email");
        SubjectQuotaRule smsRule =
                subjectRule("login-sms-subject-hour", VerificationChannel.SMS, 5, Duration.ofHours(1));
        VerificationChannel voice = VerificationChannel.of("voice");
        IssueContext voiceContext = IssueContext.of(
                new VerificationKey("account", "login", "user@example.com"), voice, VerificationPolicy.defaults());
        SubjectQuotaRule voiceRule = subjectRule("login-voice-subject-hour", voice, 5, Duration.ofHours(1));

        assertTrue(rule.appliesTo(LOGIN_EMAIL));
        assertTrue(rule.appliesTo(anotherSubject));
        assertFalse(rule.appliesTo(sms));
        assertTrue(smsRule.appliesTo(sms));
        assertTrue(voiceRule.appliesTo(voiceContext));
        assertNotEquals(rule.bucket(LOGIN_EMAIL), smsRule.bucket(sms));
        assertNotEquals(rule.bucket(LOGIN_EMAIL), voiceRule.bucket(voiceContext));
        assertNotEquals(rule.bucket(LOGIN_EMAIL), rule.bucket(anotherSubject));
        assertFalse(rule.appliesTo(context("account", "register", "user@example.com", "email")));
    }

    @Test
    void multipleSubjectWindowsAreSubmittedTogether() {
        SubjectQuotaRule hourly = subjectRule("login-email-subject-hour", 5, Duration.ofHours(1));
        SubjectQuotaRule daily = subjectRule("login-email-subject-day", 10, Duration.ofDays(1));
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueLimitManager manager = new IssueLimitManager(List.of(hourly, daily), (quotas, requestedAt) -> {
            captured.set(quotas);
            return new IssueLimitResult.Allowed();
        });

        manager.acquire(LOGIN_EMAIL, Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(
                List.of("login-email-subject-hour", "login-email-subject-day"),
                captured.get().stream().map(IssueLimitQuota::ruleId).toList());
    }

    @Test
    void subjectRuleWithOneIssueEnforcesResendCooldown() {
        SubjectQuotaRule cooldown = subjectRule("login-email-resend-cooldown", 1, Duration.ofMinutes(1));
        Instant firstIssue = Instant.parse("2026-01-01T00:00:00Z");
        IssueLimitManager manager = new IssueLimitManager(List.of(cooldown), new InMemoryIssueLimitStore());

        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(LOGIN_EMAIL, firstIssue));
        IssueLimitResult.Throttled throttled = assertInstanceOf(
                IssueLimitResult.Throttled.class, manager.acquire(LOGIN_EMAIL, firstIssue.plusSeconds(59)));
        assertEquals(Duration.ofSeconds(1), throttled.retryAfter());
        assertInstanceOf(IssueLimitResult.Allowed.class, manager.acquire(LOGIN_EMAIL, firstIssue.plusSeconds(60)));
    }

    @Test
    void buildersCreateTheSameValuesAsCanonicalConstructors() {
        assertEquals(
                new NamespaceQuotaRule(
                        "account-email-hour", "account", VerificationChannel.EMAIL, 100, Duration.ofHours(1)),
                NamespaceQuotaRule.builder()
                        .id("account-email-hour")
                        .namespace("account")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(100)
                        .window(Duration.ofHours(1))
                        .build());
        assertEquals(
                new PurposeQuotaRule(
                        "login-email-minute", "account", "login", VerificationChannel.EMAIL, 20, Duration.ofMinutes(1)),
                PurposeQuotaRule.builder()
                        .id("login-email-minute")
                        .namespace("account")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(20)
                        .window(Duration.ofMinutes(1))
                        .build());
        assertEquals(
                new SubjectQuotaRule(
                        "login-email-subject-hour",
                        "account",
                        "login",
                        VerificationChannel.EMAIL,
                        5,
                        Duration.ofHours(1)),
                subjectRule("login-email-subject-hour", 5, Duration.ofHours(1)));
    }

    @Test
    void rejectsMissingAndInvalidDefinitions() {
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .namespace("account")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .id("login-email-subject-hour")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .id("login-email-subject-hour")
                        .namespace("account")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .id("login-email-subject-hour")
                        .namespace("account")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .id("login-email-subject-hour")
                        .namespace("account")
                        .purpose("login")
                        .channel(VerificationChannel.EMAIL)
                        .maxIssues(5)
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectQuotaRule.builder()
                        .id("login-email-subject-hour")
                        .namespace("account")
                        .purpose("login")
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamespaceQuotaRule(
                        "Account-Hour", "account", VerificationChannel.EMAIL, 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamespaceQuotaRule(
                        "account-email-hour", "user_account", VerificationChannel.EMAIL, 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PurposeQuotaRule(
                        "login-email-hour",
                        "account",
                        "user_login",
                        VerificationChannel.EMAIL,
                        1,
                        Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SubjectQuotaRule(
                        "login-email-hour", "account", "login", VerificationChannel.EMAIL, 0, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SubjectQuotaRule(
                        "login-email-hour", "account", "login", VerificationChannel.EMAIL, 1, Duration.ZERO));
        assertThrows(
                NullPointerException.class,
                () -> new SubjectQuotaRule("login-email-hour", "account", "login", null, 1, Duration.ofHours(1)));
    }

    @Test
    void rejectsNullContexts() {
        NamespaceQuotaRule namespace = new NamespaceQuotaRule(
                "account-email-hour", "account", VerificationChannel.EMAIL, 100, Duration.ofHours(1));
        PurposeQuotaRule purpose = new PurposeQuotaRule(
                "login-email-hour", "account", "login", VerificationChannel.EMAIL, 50, Duration.ofHours(1));
        SubjectQuotaRule subject = subjectRule("login-email-subject-hour", 5, Duration.ofHours(1));

        assertThrows(NullPointerException.class, () -> namespace.appliesTo(null));
        assertThrows(NullPointerException.class, () -> namespace.bucket(null));
        assertThrows(NullPointerException.class, () -> purpose.appliesTo(null));
        assertThrows(NullPointerException.class, () -> purpose.bucket(null));
        assertThrows(NullPointerException.class, () -> subject.appliesTo(null));
        assertThrows(NullPointerException.class, () -> subject.bucket(null));
    }

    private static SubjectQuotaRule subjectRule(String id, int maxIssues, Duration window) {
        return subjectRule(id, VerificationChannel.EMAIL, maxIssues, window);
    }

    private static SubjectQuotaRule subjectRule(
            String id, VerificationChannel channel, int maxIssues, Duration window) {
        return SubjectQuotaRule.builder()
                .id(id)
                .namespace("account")
                .purpose("login")
                .channel(channel)
                .maxIssues(maxIssues)
                .window(window)
                .build();
    }

    private static IssueContext context(String namespace, String purpose, String subject, String channel) {
        return IssueContext.of(
                new VerificationKey(namespace, purpose, subject),
                VerificationChannel.of(channel),
                VerificationPolicy.defaults());
    }
}
