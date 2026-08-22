package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueContextResolver;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
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
            assertThat(context).doesNotHaveBean(IssueRateLimitProperties.class);
            assertThat(context).doesNotHaveBean(IssueContextResolver.class);
            assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
            assertThat(context).doesNotHaveBean(IssueRateLimiter.class);
        });
    }

    @Test
    void configuresDefaultInMemoryRateLimitingWhenVerificationIsEnabled() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IssueRateLimitProperties.class);
            assertThat(context).hasSingleBean(IssueContextResolver.class);
            assertThat(context).hasSingleBean(IssueRateLimitRule.class);
            assertThat(context).hasSingleBean(IssueRateLimitStore.class);
            assertThat(context.getBean(IssueRateLimitStore.class)).isInstanceOf(InMemoryIssueRateLimitStore.class);
            assertThat(context).hasSingleBean(IssueRateLimiter.class);
            assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
        });
    }

    @Test
    void customResolverSuppliesAttributesToDiscoveredRules() {
        VerificationKey key = new VerificationKey("account", "login", "user@example.com");
        IssueContextResolver resolver = candidate -> IssueContext.of(candidate).with("ip-address", "203.0.113.10");
        IssueRateLimitRule rule = IssueRateLimitRule.of(
                "login-ip-hour",
                context -> IssueLimitBucket.of(context.attribute("ip-address").orElseThrow()),
                10,
                Duration.ofHours(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(IssueContextResolver.class, () -> resolver)
                .withBean("ipRule", IssueRateLimitRule.class, () -> rule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IssueContextResolver.class)).isSameAs(resolver);
                    assertThat(context).getBeans(IssueRateLimitRule.class).hasSize(2);
                    assertThat(context.getBean(IssueRateLimiter.class).acquire(key, Instant.EPOCH))
                            .isInstanceOf(IssueLimitResult.Allowed.class);
                });
    }

    @Test
    void zeroIntervalDisablesOnlyDefaultRule() {
        IssueRateLimitRule customRule = IssueRateLimitRule.of(
                "subject-hour", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofHours(1));

        contextRunner
                .withPropertyValues(
                        "ringo.boot.verification.enabled=true", "ringo.boot.verification.issue-rate-limit.interval=0")
                .withBean("customRule", IssueRateLimitRule.class, () -> customRule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueRateLimitRule.class);
                    assertThat(context.getBean(IssueRateLimitRule.class)).isSameAs(customRule);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
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
}
