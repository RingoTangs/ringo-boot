package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.ringotangs.ringoboot.autoconfigure.verification.IssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisIssueRateLimitAutoConfigurationTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisIssueRateLimitAutoConfiguration.class, IssueRateLimitAutoConfiguration.class))
            .withPropertyValues(
                    "spring.application.name=test-application",
                    "ringo.boot.verification.enabled=true",
                    "ringo.boot.verification.store=redis")
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void configuresRedisRateLimitStoreAndManager() {
        contextRunner
                .withPropertyValues("ringo.boot.verification.redis.secret=" + SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IssueRateLimitStore.class);
                    assertThat(context.getBean(IssueRateLimitStore.class)).isInstanceOf(RedisIssueRateLimitStore.class);
                    assertThat(context).hasSingleBean(IssueRateLimiter.class);
                    assertThat(context.getBean(IssueRateLimiter.class)).isInstanceOf(IssueRateLimitManager.class);
                });
    }

    @Test
    void failsWhenSharedSecretIsMissing() {
        contextRunner.run(context -> assertThat(context.getStartupFailure())
                .hasRootCauseMessage("ringo.boot.verification.redis.secret must be configured"));
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
}
