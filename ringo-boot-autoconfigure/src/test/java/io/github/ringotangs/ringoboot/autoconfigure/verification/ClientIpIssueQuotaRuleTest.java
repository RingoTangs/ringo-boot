package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ClientIpIssueQuotaRuleTest {

    private static final VerificationPolicy POLICY = VerificationPolicy.defaults();

    @Test
    void matchesConfiguredBusinessScopeWithoutInspectingClientIp() {
        ClientIpIssueQuotaRule rule = rule();

        assertThat(rule.matches(context("account", "login", VerificationChannel.EMAIL, "user-1")))
                .isTrue();
        assertThat(rule.matches(context("account", "register", VerificationChannel.EMAIL, "user-1")))
                .isFalse();
        assertThat(rule.matches(context("profile", "login", VerificationChannel.EMAIL, "user-1")))
                .isFalse();
        assertThat(rule.matches(context("account", "login", VerificationChannel.SMS, "user-1")))
                .isFalse();
    }

    @Test
    void sharesBucketAcrossSubjectsFromSameClientIp() {
        ClientIpIssueQuotaRule rule = rule();
        IssueContext first =
                withClientIp(context("account", "login", VerificationChannel.EMAIL, "user-1"), "203.0.113.10");
        IssueContext second =
                withClientIp(context("account", "login", VerificationChannel.EMAIL, "user-2"), "203.0.113.10");

        assertThat(rule.bucket(first)).isEqualTo(rule.bucket(second));
        assertThat(rule.bucket(first))
                .isEqualTo(IssueLimitBucket.of("account", "login", VerificationChannel.EMAIL.value(), "203.0.113.10"));
    }

    @Test
    void separatesBucketsByClientIp() {
        ClientIpIssueQuotaRule rule = rule();
        IssueContext context = context("account", "login", VerificationChannel.EMAIL, "user-1");

        assertThat(rule.bucket(withClientIp(context, "203.0.113.10")))
                .isNotEqualTo(rule.bucket(withClientIp(context, "203.0.113.11")));
    }

    @Test
    void rejectsMissingClientIpAfterRuleMatches() {
        ClientIpIssueQuotaRule rule = rule();
        IssueContext context = context("account", "login", VerificationChannel.EMAIL, "user-1");

        assertThat(rule.matches(context)).isTrue();
        assertThatIllegalStateException()
                .isThrownBy(() -> rule.bucket(context))
                .withMessage("required issue context attribute is missing: client-ip");
    }

    @Test
    void validatesRuleDefinition() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ClientIpIssueQuotaRule(
                        "login_ip_hour", "account", "login", VerificationChannel.EMAIL, 1, Duration.ofHours(1)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ClientIpIssueQuotaRule(
                        "login-ip-hour", "account", "login", VerificationChannel.EMAIL, 0, Duration.ofHours(1)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ClientIpIssueQuotaRule(
                        "login-ip-hour", "account", "login", VerificationChannel.EMAIL, 1, Duration.ZERO));
    }

    @Test
    void builderRequiresEveryField() {
        assertThatNullPointerException()
                .isThrownBy(() -> ClientIpIssueQuotaRule.builder().build())
                .withMessage("id must be configured");
    }

    private static ClientIpIssueQuotaRule rule() {
        return ClientIpIssueQuotaRule.builder()
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
        return context.with(ClientIpContributor.ATTRIBUTE_NAME, clientIp);
    }
}
