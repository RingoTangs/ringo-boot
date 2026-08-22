package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.limit.GlobalIssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueRateLimitPropertiesTest {

    private static final IssueContext LOGIN =
            IssueContext.of(new VerificationKey("account", "login", "user@example.com"));
    private static final IssueContext OTHER_SUBJECT =
            IssueContext.of(new VerificationKey("account", "login", "other@example.com"));
    private static final IssueContext REGISTER =
            IssueContext.of(new VerificationKey("account", "register", "user@example.com"));
    private static final IssueContext PAYMENT =
            IssueContext.of(new VerificationKey("payment", "confirm", "user@example.com"));

    @Test
    void convertsAllConfiguredScopesToRules() {
        IssueRateLimitProperties properties = new IssueRateLimitProperties();
        properties.setRules(List.of(
                rule("application-minute", IssueRateLimitProperties.Scope.GLOBAL, null, null),
                rule("account-hour", IssueRateLimitProperties.Scope.NAMESPACE, "account", null),
                rule("account-login-hour", IssueRateLimitProperties.Scope.PURPOSE, "account", "login"),
                rule("account-login-subject-hour", IssueRateLimitProperties.Scope.SUBJECT, "account", "login")));

        List<IssueRateLimitRule> rules = properties.toRules();

        assertThat(rules).hasSize(4);
        assertThat(rules.getFirst()).isInstanceOf(GlobalIssueRateLimitRule.class);
        assertThat(rules.get(1).matches(LOGIN)).isTrue();
        assertThat(rules.get(1).matches(PAYMENT)).isFalse();
        assertThat(rules.get(1).bucket(LOGIN)).isEqualTo(IssueLimitBucket.of("account"));
        assertThat(rules.get(2).matches(LOGIN)).isTrue();
        assertThat(rules.get(2).matches(REGISTER)).isFalse();
        assertThat(rules.get(2).bucket(LOGIN)).isEqualTo(IssueLimitBucket.of("account", "login"));
        assertThat(rules.get(3).matches(LOGIN)).isTrue();
        assertThat(rules.get(3).matches(REGISTER)).isFalse();
        assertThat(rules.get(3).bucket(LOGIN)).isEqualTo(IssueLimitBucket.of("account", "login", "user@example.com"));
        assertThat(rules.get(3).bucket(OTHER_SUBJECT))
                .isEqualTo(IssueLimitBucket.of("account", "login", "other@example.com"));
    }

    @Test
    void rejectsFieldsThatDoNotBelongToScope() {
        IssueRateLimitProperties global =
                properties(rule("application-minute", IssueRateLimitProperties.Scope.GLOBAL, "account", null));
        IssueRateLimitProperties namespace =
                properties(rule("account-hour", IssueRateLimitProperties.Scope.NAMESPACE, "account", "login"));

        assertThatThrownBy(global::toRules)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("namespace must not be configured for scope global");
        assertThatThrownBy(namespace::toRules)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purpose must not be configured for scope namespace");
    }

    @Test
    void rejectsMissingAndInvalidRequiredFields() {
        IssueRateLimitProperties.Rule missingId = rule(null, IssueRateLimitProperties.Scope.GLOBAL, null, null);
        IssueRateLimitProperties.Rule missingScope = rule("application-minute", null, null, null);
        IssueRateLimitProperties.Rule missingNamespace =
                rule("account-hour", IssueRateLimitProperties.Scope.NAMESPACE, null, null);
        IssueRateLimitProperties.Rule missingPurpose =
                rule("account-login-hour", IssueRateLimitProperties.Scope.PURPOSE, "account", null);
        IssueRateLimitProperties.Rule invalidNamespace =
                rule("account-hour", IssueRateLimitProperties.Scope.NAMESPACE, "User_Account", null);

        assertThatThrownBy(() -> properties(missingId).toRules()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> properties(missingScope).toRules()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> properties(missingNamespace).toRules()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> properties(missingPurpose).toRules()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> properties(invalidNamespace).toRules())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase kebab-case");
    }

    @Test
    void rejectsInvalidQuotaDefinitions() {
        IssueRateLimitProperties.Rule invalidId =
                rule("application_minute", IssueRateLimitProperties.Scope.GLOBAL, null, null);
        IssueRateLimitProperties.Rule invalidCount =
                rule("application-minute", IssueRateLimitProperties.Scope.GLOBAL, null, null);
        invalidCount.setMaxIssues(0);
        IssueRateLimitProperties.Rule invalidWindow =
                rule("application-minute", IssueRateLimitProperties.Scope.GLOBAL, null, null);
        invalidWindow.setWindow(Duration.ZERO);

        assertThatThrownBy(() -> properties(invalidId).toRules()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(invalidCount).toRules()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(invalidWindow).toRules()).isInstanceOf(IllegalArgumentException.class);
    }

    private IssueRateLimitProperties properties(IssueRateLimitProperties.Rule rule) {
        IssueRateLimitProperties properties = new IssueRateLimitProperties();
        properties.setRules(List.of(rule));
        return properties;
    }

    private IssueRateLimitProperties.Rule rule(
            String id, IssueRateLimitProperties.Scope scope, String namespace, String purpose) {
        IssueRateLimitProperties.Rule rule = new IssueRateLimitProperties.Rule();
        rule.setId(id);
        rule.setScope(scope);
        rule.setNamespace(namespace);
        rule.setPurpose(purpose);
        rule.setMaxIssues(10);
        rule.setWindow(Duration.ofHours(1));
        return rule;
    }
}
