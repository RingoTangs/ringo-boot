package io.github.ringotangs.ringoboot.autoconfigure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitStore;
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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class IssueRateLimitAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

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
        IssueRateLimitRule customRule = new TestIssueRateLimitRule(
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
        IssueRateLimitRule rule = new TestIssueRateLimitRule(
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
        IssueRateLimitRule applicationRule = new TestIssueRateLimitRule(
                "application-minute", context -> IssueLimitBucket.of("application"), 1, Duration.ofMinutes(1));
        IssueRateLimitRule customRule = new TestIssueRateLimitRule(
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
        IssueRateLimitRule first = new TestIssueRateLimitRule(
                "application-minute", context -> IssueLimitBucket.of("custom"), 100, Duration.ofHours(1));
        IssueRateLimitRule second = new TestIssueRateLimitRule(
                "application-minute", context -> IssueLimitBucket.of("other"), 10, Duration.ofMinutes(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("firstRule", IssueRateLimitRule.class, () -> first)
                .withBean("secondRule", IssueRateLimitRule.class, () -> second)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("duplicate issue rate limit rule id: application-minute"));
    }

    @Test
    void permitsAllIssuesWithoutConfiguringRedisStoreWhenRulesAreMissing() {
        redisContextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IssueRateLimitRule.class);
            assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
            assertThat(context).hasSingleBean(IssueRateLimiter.class);
        });
    }

    @Test
    void configuresRedisRateLimitStoreAndManager() {
        redisContextRunnerWithRule()
                .withBean(VerificationHmacKey.class, IssueRateLimitAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueRateLimitStore.class);
                    assertThat(context.getBean(IssueRateLimitStore.class)).isInstanceOf(RedisIssueRateLimitStore.class);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                });
    }

    @Test
    void failsWhenRedisHmacKeyIsMissing() {
        redisContextRunnerWithRule()
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void failsWhenMultipleRedisHmacKeysAreConfigured() {
        redisContextRunnerWithRule()
                .withBean("firstHmacKey", VerificationHmacKey.class, IssueRateLimitAutoConfigurationTest::hmacKey)
                .withBean("secondHmacKey", VerificationHmacKey.class, IssueRateLimitAutoConfigurationTest::hmacKey)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class));
    }

    @Test
    void customRateLimitStoreOverridesRedisDefault() {
        IssueRateLimitStore store = (quotas, requestedAt) -> new IssueLimitResult.Allowed();

        redisContextRunnerWithRule()
                .withBean(IssueRateLimitStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IssueRateLimitStore.class)).isSameAs(store);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                });
    }

    private static ApplicationContextRunner redisContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IssueRateLimitAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=test-application",
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
    }

    private static ApplicationContextRunner redisContextRunnerWithRule() {
        return redisContextRunner()
                .withBean("issueRateLimitRule", IssueRateLimitRule.class, IssueRateLimitAutoConfigurationTest::rule);
    }

    private static VerificationHmacKey hmacKey() {
        return VerificationHmacKey.fromBase64(SECRET);
    }

    private static IssueRateLimitRule rule() {
        return new TestIssueRateLimitRule(
                "subject-minute", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofMinutes(1));
    }
}
