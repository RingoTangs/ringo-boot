package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BuiltInIssueQuotaRuleTest {

    private static final IssueContext LOGIN_EMAIL = context("account", "login", "user@example.com", "email");

    @Test
    void namespaceRuleAggregatesAllPurposesChannelsAndSubjects() {
        NamespaceIssueQuotaRule rule = NamespaceIssueQuotaRule.builder()
                .id("account-hour")
                .namespace("account")
                .maxIssues(100)
                .window(Duration.ofHours(1))
                .build();

        assertEquals(IssueLimitBucket.of("account"), rule.bucket(LOGIN_EMAIL));
        assertTrue(rule.matches(context("account", "register", "other@example.com", "email")));
        assertTrue(rule.matches(context("account", "login", "13800138000", "sms")));
        assertFalse(rule.matches(context("payment", "login", "user@example.com", "email")));
    }

    @Test
    void purposeRuleAggregatesAllChannelsAndSubjectsWithinOnePurpose() {
        PurposeIssueQuotaRule rule = PurposeIssueQuotaRule.builder()
                .id("login-minute")
                .namespace("account")
                .purpose("login")
                .maxIssues(20)
                .window(Duration.ofMinutes(1))
                .build();

        assertEquals(IssueLimitBucket.of("account", "login"), rule.bucket(LOGIN_EMAIL));
        assertTrue(rule.matches(context("account", "login", "13800138000", "sms")));
        assertFalse(rule.matches(context("account", "register", "user@example.com", "email")));
        assertFalse(rule.matches(context("payment", "login", "user@example.com", "email")));
    }

    @Test
    void subjectRuleSharesQuotaAcrossChannelsAndSeparatesSubjects() {
        SubjectIssueQuotaRule rule = subjectRule("login-subject-hour", 5, Duration.ofHours(1));
        IssueContext sms = context("account", "login", "user@example.com", "sms");
        IssueContext anotherSubject = context("account", "login", "other@example.com", "email");

        assertTrue(rule.matches(LOGIN_EMAIL));
        assertTrue(rule.matches(sms));
        assertEquals(rule.bucket(LOGIN_EMAIL), rule.bucket(sms));
        assertFalse(rule.bucket(LOGIN_EMAIL).equals(rule.bucket(anotherSubject)));
        assertFalse(rule.matches(context("account", "register", "user@example.com", "email")));
    }

    @Test
    void multipleSubjectWindowsAreSubmittedTogether() {
        SubjectIssueQuotaRule hourly = subjectRule("login-subject-hour", 5, Duration.ofHours(1));
        SubjectIssueQuotaRule daily = subjectRule("login-subject-day", 10, Duration.ofDays(1));
        AtomicReference<List<IssueLimitQuota>> captured = new AtomicReference<>();
        IssueRateLimitManager manager = new IssueRateLimitManager(List.of(hourly, daily), (quotas, requestedAt) -> {
            captured.set(quotas);
            return new IssueLimitResult.Allowed();
        });

        manager.acquire(LOGIN_EMAIL, Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(
                List.of("login-subject-hour", "login-subject-day"),
                captured.get().stream().map(IssueLimitQuota::ruleId).toList());
    }

    @Test
    void buildersCreateTheSameValuesAsCanonicalConstructors() {
        assertEquals(
                new NamespaceIssueQuotaRule("account-hour", "account", 100, Duration.ofHours(1)),
                NamespaceIssueQuotaRule.builder()
                        .id("account-hour")
                        .namespace("account")
                        .maxIssues(100)
                        .window(Duration.ofHours(1))
                        .build());
        assertEquals(
                new PurposeIssueQuotaRule("login-minute", "account", "login", 20, Duration.ofMinutes(1)),
                PurposeIssueQuotaRule.builder()
                        .id("login-minute")
                        .namespace("account")
                        .purpose("login")
                        .maxIssues(20)
                        .window(Duration.ofMinutes(1))
                        .build());
        assertEquals(
                new SubjectIssueQuotaRule("login-subject-hour", "account", "login", 5, Duration.ofHours(1)),
                subjectRule("login-subject-hour", 5, Duration.ofHours(1)));
    }

    @Test
    void rejectsMissingAndInvalidDefinitions() {
        assertThrows(
                NullPointerException.class,
                () -> SubjectIssueQuotaRule.builder()
                        .namespace("account")
                        .purpose("login")
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectIssueQuotaRule.builder()
                        .id("login-subject-hour")
                        .purpose("login")
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectIssueQuotaRule.builder()
                        .id("login-subject-hour")
                        .namespace("account")
                        .maxIssues(5)
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectIssueQuotaRule.builder()
                        .id("login-subject-hour")
                        .namespace("account")
                        .purpose("login")
                        .window(Duration.ofHours(1))
                        .build());
        assertThrows(
                NullPointerException.class,
                () -> SubjectIssueQuotaRule.builder()
                        .id("login-subject-hour")
                        .namespace("account")
                        .purpose("login")
                        .maxIssues(5)
                        .build());
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamespaceIssueQuotaRule("Account-Hour", "account", 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamespaceIssueQuotaRule("account-hour", "user_account", 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PurposeIssueQuotaRule("login-hour", "account", "user_login", 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SubjectIssueQuotaRule("login-hour", "account", "login", 0, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SubjectIssueQuotaRule("login-hour", "account", "login", 1, Duration.ZERO));
    }

    @Test
    void rejectsNullContexts() {
        NamespaceIssueQuotaRule namespace =
                new NamespaceIssueQuotaRule("account-hour", "account", 100, Duration.ofHours(1));
        PurposeIssueQuotaRule purpose =
                new PurposeIssueQuotaRule("login-hour", "account", "login", 50, Duration.ofHours(1));
        SubjectIssueQuotaRule subject = subjectRule("login-subject-hour", 5, Duration.ofHours(1));

        assertThrows(NullPointerException.class, () -> namespace.matches(null));
        assertThrows(NullPointerException.class, () -> namespace.bucket(null));
        assertThrows(NullPointerException.class, () -> purpose.matches(null));
        assertThrows(NullPointerException.class, () -> purpose.bucket(null));
        assertThrows(NullPointerException.class, () -> subject.matches(null));
        assertThrows(NullPointerException.class, () -> subject.bucket(null));
    }

    private static SubjectIssueQuotaRule subjectRule(String id, int maxIssues, Duration window) {
        return SubjectIssueQuotaRule.builder()
                .id(id)
                .namespace("account")
                .purpose("login")
                .maxIssues(maxIssues)
                .window(window)
                .build();
    }

    private static IssueContext context(String namespace, String purpose, String subject, String channel) {
        return IssueContext.of(new VerificationKey(namespace, purpose, subject), VerificationChannel.of(channel));
    }
}
