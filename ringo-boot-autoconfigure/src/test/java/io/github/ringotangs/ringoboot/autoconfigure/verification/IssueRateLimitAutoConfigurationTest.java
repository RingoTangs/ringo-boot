package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueRateLimitRuleException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IssueRateLimitAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IssueRateLimitAutoConfiguration.class));

    @Test
    void doesNotConfigureRateLimitingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
            assertThat(context).doesNotHaveBean(IssueRateLimiter.class);
        });
    }

    @Test
    void permitsAllIssuesWhenVerificationIsEnabledWithoutRules() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IssueRateLimitRule.class);
            assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
            assertThat(context).hasSingleBean(IssueRateLimiter.class);
            IssueRateLimiter limiter = context.getBean(IssueRateLimiter.class);
            IssueContext issueContext = IssueContext.of(
                    new VerificationKey("account", "login", "user@example.com"), VerificationChannel.EMAIL);
            assertThat(limiter.acquire(issueContext, Instant.EPOCH)).isInstanceOf(IssueLimitResult.Allowed.class);
            assertThat(limiter.acquire(issueContext, Instant.EPOCH)).isInstanceOf(IssueLimitResult.Allowed.class);
        });
    }

    @Test
    void customRuleReplacesDefaultRule() {
        IssueRateLimitRule customRule = IssueRateLimitRule.of(
                "subject-hour", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofHours(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("customRule", IssueRateLimitRule.class, () -> customRule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueRateLimitRule.class);
                    assertThat(context.getBean(IssueRateLimitRule.class)).isSameAs(customRule);
                    assertThat(context).hasSingleBean(IssueRateLimitStore.class);
                    assertThat(context.getBean(IssueRateLimitStore.class))
                            .isInstanceOf(InMemoryIssueRateLimitStore.class);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                });
    }

    @Test
    void partialRuleBeanRejectsUncoveredVerificationKeys() {
        VerificationKey uncovered = new VerificationKey("payment", "confirm", "+8613800000000");
        IssueRateLimitRule rule = IssueRateLimitRule.of(
                "account-login-hour",
                context -> context.key().namespace().equals("account")
                        && context.key().purpose().equals("login"),
                context -> IssueLimitBucket.of(
                        context.key().namespace(), context.key().purpose()),
                10,
                Duration.ofHours(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("accountLoginRule", IssueRateLimitRule.class, () -> rule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(IssueRateLimiter.class)
                                    .acquire(IssueContext.of(uncovered, VerificationChannel.SMS), Instant.EPOCH))
                            .isInstanceOf(MissingIssueRateLimitRuleException.class)
                            .hasMessage("no issue rate limit rule matches namespace=payment, purpose=confirm")
                            .hasMessageNotContaining(uncovered.subject());
                });
    }

    @Test
    void customLimiterReplacesDefaultManagerAndStore() {
        IssueRateLimiter limiter = (key, requestedAt) -> new IssueLimitResult.Allowed();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(IssueRateLimiter.class, () -> limiter)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IssueRateLimiter.class)).isSameAs(limiter);
                    assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
                    assertThat(context).doesNotHaveBean(IssueRateLimitRule.class);
                });
    }

    @Test
    void collectsMultipleRuleBeans() {
        IssueRateLimitRule applicationRule = IssueRateLimitRule.of(
                "application-minute", context -> IssueLimitBucket.of("application"), 1, Duration.ofMinutes(1));
        IssueRateLimitRule customRule = IssueRateLimitRule.of(
                "custom-hour", context -> IssueLimitBucket.of("custom"), 100, Duration.ofHours(1));
        VerificationKey first = new VerificationKey("account", "login", "user@example.com");
        VerificationKey second = new VerificationKey("payment", "confirm", "+8613800000000");

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("applicationRule", IssueRateLimitRule.class, () -> applicationRule)
                .withBean("customRule", IssueRateLimitRule.class, () -> customRule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeans(IssueRateLimitRule.class).hasSize(2);
                    IssueRateLimiter limiter = context.getBean(IssueRateLimiter.class);
                    assertThat(limiter.acquire(IssueContext.of(first, VerificationChannel.EMAIL), Instant.EPOCH))
                            .isInstanceOf(IssueLimitResult.Allowed.class);
                    assertThat(limiter.acquire(
                                    IssueContext.of(second, VerificationChannel.SMS), Instant.EPOCH.plusSeconds(1)))
                            .isInstanceOf(IssueLimitResult.Throttled.class);
                });
    }

    @Test
    void rejectsDuplicateIdsAcrossRuleBeans() {
        IssueRateLimitRule first = IssueRateLimitRule.of(
                "application-minute", context -> IssueLimitBucket.of("custom"), 100, Duration.ofHours(1));
        IssueRateLimitRule second = IssueRateLimitRule.of(
                "application-minute", context -> IssueLimitBucket.of("other"), 10, Duration.ofMinutes(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("firstRule", IssueRateLimitRule.class, () -> first)
                .withBean("secondRule", IssueRateLimitRule.class, () -> second)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("duplicate issue rate limit rule id: application-minute"));
    }
}
