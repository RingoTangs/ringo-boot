package io.github.ringotangs.ringoboot.verification.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException;
import io.github.ringotangs.ringoboot.verification.limit.RuleBasedIssueLimiter;
import io.github.ringotangs.ringoboot.verification.redis.RedisIssueLimitStore;
import io.github.ringotangs.ringoboot.verification.redis.VerificationHmacKey;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class IssueLimitAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(IssueLimitAutoConfiguration.class));

    @Test
    void doesNotConfigureLimitingByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(IssueLimitStore.class);
            assertThat(context).doesNotHaveBean(IssueLimiter.class);
        });
    }

    @Test
    void permitsAllIssuesWhenVerificationIsEnabledWithoutRules() {
        contextRunner.withPropertyValues("ringo.boot.verification.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IssueLimitRule.class);
            assertThat(context).doesNotHaveBean(IssueLimitStore.class);
            assertThat(context).hasSingleBean(IssueLimiter.class);
            IssueLimiter limiter = context.getBean(IssueLimiter.class);
            IssueContext issueContext = IssueContext.of(
                    new VerificationKey("account", "login", "user@example.com"),
                    VerificationChannel.EMAIL,
                    VerificationPolicy.defaults());
            assertThat(limiter.acquire(issueContext, Instant.EPOCH)).isInstanceOf(IssueLimitResult.Allowed.class);
            assertThat(limiter.acquire(issueContext, Instant.EPOCH)).isInstanceOf(IssueLimitResult.Allowed.class);
        });
    }

    @Test
    void customRuleReplacesDefaultRule() {
        IssueLimitRule customRule = new TestIssueLimitRule(
                "subject-hour", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofHours(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("customRule", IssueLimitRule.class, () -> customRule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueLimitRule.class);
                    assertThat(context.getBean(IssueLimitRule.class)).isSameAs(customRule);
                    assertThat(context).hasSingleBean(IssueLimitStore.class);
                    assertThat(context.getBean(IssueLimitStore.class)).isInstanceOf(InMemoryIssueLimitStore.class);
                    assertThat(context).hasSingleBean(IssueLimiter.class);
                    assertThat(context.getBean(IssueLimiter.class)).isInstanceOf(RuleBasedIssueLimiter.class);
                });
    }

    @Test
    void partialRuleBeanRejectsUncoveredVerificationKeys() {
        VerificationKey uncovered = new VerificationKey("payment", "confirm", "+8613800000000");
        IssueLimitRule rule = new TestIssueLimitRule(
                "account-login-hour",
                context -> context.key().namespace().equals("account")
                        && context.key().purpose().equals("login"),
                context -> IssueLimitBucket.of(
                        context.key().namespace(), context.key().purpose()),
                10,
                Duration.ofHours(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("accountLoginRule", IssueLimitRule.class, () -> rule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getBean(IssueLimiter.class)
                                    .acquire(
                                            IssueContext.of(
                                                    uncovered, VerificationChannel.SMS, VerificationPolicy.defaults()),
                                            Instant.EPOCH))
                            .isInstanceOf(MissingIssueLimitRuleException.class)
                            .hasMessage("no issue rate limit rule matches namespace=payment, purpose=confirm")
                            .hasMessageNotContaining(uncovered.subject());
                });
    }

    @Test
    void customLimiterReplacesDefaultManagerAndStore() {
        IssueLimiter limiter = (key, requestedAt) -> new IssueLimitResult.Allowed();

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean(IssueLimiter.class, () -> limiter)
                .withBean("customRule", IssueLimitRule.class, IssueLimitAutoConfigurationTest::rule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IssueLimiter.class)).isSameAs(limiter);
                    assertThat(context).doesNotHaveBean(IssueLimitStore.class);
                    assertThat(context).hasSingleBean(IssueLimitRule.class);
                });
    }

    @Test
    void collectsMultipleRuleBeans() {
        IssueLimitRule applicationRule = new TestIssueLimitRule(
                "application-minute", context -> IssueLimitBucket.of("application"), 1, Duration.ofMinutes(1));
        IssueLimitRule customRule = new TestIssueLimitRule(
                "custom-hour", context -> IssueLimitBucket.of("custom"), 100, Duration.ofHours(1));
        VerificationKey first = new VerificationKey("account", "login", "user@example.com");
        VerificationKey second = new VerificationKey("payment", "confirm", "+8613800000000");

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("applicationRule", IssueLimitRule.class, () -> applicationRule)
                .withBean("customRule", IssueLimitRule.class, () -> customRule)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBeans(IssueLimitRule.class).hasSize(2);
                    IssueLimiter limiter = context.getBean(IssueLimiter.class);
                    assertThat(limiter.acquire(
                                    IssueContext.of(first, VerificationChannel.EMAIL, VerificationPolicy.defaults()),
                                    Instant.EPOCH))
                            .isInstanceOf(IssueLimitResult.Allowed.class);
                    assertThat(limiter.acquire(
                                    IssueContext.of(second, VerificationChannel.SMS, VerificationPolicy.defaults()),
                                    Instant.EPOCH.plusSeconds(1)))
                            .isInstanceOf(IssueLimitResult.Throttled.class);
                });
    }

    @Test
    void rejectsDuplicateIdsAcrossRuleBeans() {
        IssueLimitRule first = new TestIssueLimitRule(
                "application-minute", context -> IssueLimitBucket.of("custom"), 100, Duration.ofHours(1));
        IssueLimitRule second = new TestIssueLimitRule(
                "application-minute", context -> IssueLimitBucket.of("other"), 10, Duration.ofMinutes(1));

        contextRunner
                .withPropertyValues("ringo.boot.verification.enabled=true")
                .withBean("firstRule", IssueLimitRule.class, () -> first)
                .withBean("secondRule", IssueLimitRule.class, () -> second)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("duplicate issue rate limit rule id: application-minute"));
    }

    @Test
    void permitsAllIssuesWithoutConfiguringRedisStoreWhenRulesAreMissing() {
        redisContextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IssueLimitRule.class);
            assertThat(context).doesNotHaveBean(IssueLimitStore.class);
            assertThat(context).hasSingleBean(IssueLimiter.class);
        });
    }

    @Test
    void configuresRedisLimitStoreAndManager() {
        redisContextRunnerWithRule()
                .withBean(VerificationHmacKey.class, IssueLimitAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueLimitStore.class);
                    assertThat(context.getBean(IssueLimitStore.class)).isInstanceOf(RedisIssueLimitStore.class);
                    assertThat(context).hasSingleBean(IssueLimiter.class);
                    assertThat(context.getBean(IssueLimiter.class)).isInstanceOf(RuleBasedIssueLimiter.class);
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
                .withBean("firstHmacKey", VerificationHmacKey.class, IssueLimitAutoConfigurationTest::hmacKey)
                .withBean("secondHmacKey", VerificationHmacKey.class, IssueLimitAutoConfigurationTest::hmacKey)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class));
    }

    @Test
    void customLimitStoreOverridesRedisDefault() {
        IssueLimitStore store = (quotas, requestedAt) -> new IssueLimitResult.Allowed();

        redisContextRunnerWithRule()
                .withBean(IssueLimitStore.class, () -> store)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IssueLimitStore.class)).isSameAs(store);
                    assertThat(context).hasSingleBean(IssueLimiter.class);
                });
    }

    private static ApplicationContextRunner redisContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IssueLimitAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=test-application",
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
    }

    private static ApplicationContextRunner redisContextRunnerWithRule() {
        return redisContextRunner()
                .withBean("issueLimitRule", IssueLimitRule.class, IssueLimitAutoConfigurationTest::rule);
    }

    private static VerificationHmacKey hmacKey() {
        return VerificationHmacKey.fromBase64(SECRET);
    }

    private static IssueLimitRule rule() {
        return new TestIssueLimitRule(
                "subject-minute", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofMinutes(1));
    }
}
