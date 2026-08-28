package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.autoconfigure.verification.IssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationHmacKey;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisIssueRateLimitAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final ApplicationContextRunner contextRunner = baseContextRunner()
            .withBean("issueRateLimitRule", IssueRateLimitRule.class, RedisIssueRateLimitAutoConfigurationTest::rule);

    private static ApplicationContextRunner baseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        RedisIssueRateLimitAutoConfiguration.class, IssueRateLimitAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=test-application",
                        "ringo.boot.verification.enabled=true",
                        "ringo.boot.verification.store=redis")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
    }

    @Test
    void permitsAllIssuesWithoutConfiguringRedisStoreWhenRulesAreMissing() {
        baseContextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IssueRateLimitRule.class);
            assertThat(context).doesNotHaveBean(IssueRateLimitStore.class);
            assertThat(context).hasSingleBean(IssueRateLimiter.class);
        });
    }

    @Test
    void configuresRedisRateLimitStoreAndManager() {
        contextRunner
                .withBean(VerificationHmacKey.class, RedisIssueRateLimitAutoConfigurationTest::hmacKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueRateLimitStore.class);
                    assertThat(context.getBean(IssueRateLimitStore.class)).isInstanceOf(RedisIssueRateLimitStore.class);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                });
    }

    @Test
    void failsWhenHmacKeyIsMissing() {
        contextRunner.run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @Test
    void failsWhenMultipleHmacKeysAreConfigured() {
        contextRunner
                .withBean("firstHmacKey", VerificationHmacKey.class, RedisIssueRateLimitAutoConfigurationTest::hmacKey)
                .withBean("secondHmacKey", VerificationHmacKey.class, RedisIssueRateLimitAutoConfigurationTest::hmacKey)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class));
    }

    @Test
    void customRateLimitStoreOverridesRedisDefault() {
        IssueRateLimitStore store = (quotas, requestedAt) -> new IssueLimitResult.Allowed();

        contextRunner.withBean(IssueRateLimitStore.class, () -> store).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(IssueRateLimitStore.class)).isSameAs(store);
            assertThat(context).hasSingleBean(IssueRateLimiter.class);
        });
    }

    private static VerificationHmacKey hmacKey() {
        return VerificationHmacKey.fromBase64(SECRET);
    }

    private static IssueRateLimitRule rule() {
        return IssueRateLimitRule.of(
                "subject-minute", context -> IssueLimitBucket.of(context.key().subject()), 1, Duration.ofMinutes(1));
    }
}
