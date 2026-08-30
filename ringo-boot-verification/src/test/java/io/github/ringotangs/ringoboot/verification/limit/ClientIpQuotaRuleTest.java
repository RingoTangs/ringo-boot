package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ClientIpQuotaRuleTest {

    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();

    @Test
    void appliesToConfiguredBusinessScopeWithoutInspectingClientIp() {
        ClientIpQuotaRule rule = rule();

        assertTrue(rule.appliesTo(context("account", "login", VerificationChannel.EMAIL, "user-1")));
        assertFalse(rule.appliesTo(context("account", "register", VerificationChannel.EMAIL, "user-1")));
        assertFalse(rule.appliesTo(context("profile", "login", VerificationChannel.EMAIL, "user-1")));
        assertFalse(rule.appliesTo(context("account", "login", VerificationChannel.SMS, "user-1")));
    }

    @Test
    void sharesBucketAcrossSubjectsFromSameClientIp() {
        ClientIpQuotaRule rule = rule();
        IssueContext first =
                withClientIp(context("account", "login", VerificationChannel.EMAIL, "user-1"), "203.0.113.10");
        IssueContext second =
                withClientIp(context("account", "login", VerificationChannel.EMAIL, "user-2"), "203.0.113.10");

        assertEquals(rule.bucket(first), rule.bucket(second));
        assertEquals(
                IssueLimitBucket.of("account", "login", VerificationChannel.EMAIL.value(), "203.0.113.10"),
                rule.bucket(first));
    }

    @Test
    void separatesBucketsByClientIp() {
        ClientIpQuotaRule rule = rule();
        IssueContext context = context("account", "login", VerificationChannel.EMAIL, "user-1");

        assertNotEquals(
                rule.bucket(withClientIp(context, "203.0.113.10")), rule.bucket(withClientIp(context, "203.0.113.11")));
    }

    @Test
    void rejectsMissingClientIpAfterRuleMatches() {
        ClientIpQuotaRule rule = rule();
        IssueContext context = context("account", "login", VerificationChannel.EMAIL, "user-1");

        assertTrue(rule.appliesTo(context));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> rule.bucket(context));
        assertEquals("required issue context attribute is missing: client-ip", exception.getMessage());
    }

    @Test
    void validatesRuleDefinition() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientIpQuotaRule(
                        "login_ip_hour", "account", "login", VerificationChannel.EMAIL, 1, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientIpQuotaRule(
                        "login-ip-hour", "account", "login", VerificationChannel.EMAIL, 0, Duration.ofHours(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientIpQuotaRule(
                        "login-ip-hour", "account", "login", VerificationChannel.EMAIL, 1, Duration.ZERO));
    }

    @Test
    void builderRequiresEveryField() {
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> ClientIpQuotaRule.builder().build());
        assertEquals("id must be configured", exception.getMessage());
    }

    private static ClientIpQuotaRule rule() {
        return ClientIpQuotaRule.builder()
                .id("login-ip-hour")
                .namespace("account")
                .purpose("login")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofHours(1))
                .build();
    }

    private static IssueContext context(String namespace, String purpose, VerificationChannel channel, String subject) {
        return IssueContext.of(new VerificationKey(namespace, purpose, subject), channel, POLICY);
    }

    private static IssueContext withClientIp(IssueContext context, String clientIp) {
        return context.with(ClientIpQuotaRule.ATTRIBUTE_NAME, clientIp);
    }
}
