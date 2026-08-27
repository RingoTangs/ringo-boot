package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.limit.GlobalIssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置验证码签发限流规则。
 *
 * <p>应用只需向 Spring 容器注册需要启用的规则，无需在配置文件中声明额度。</p>
 */
@Configuration(proxyBeanMethods = false)
class IssueRateLimitConfiguration {

    /**
     * 创建同一验证码键的签发冷却规则。
     *
     * @return 同一验证码键的签发冷却规则
     */
    @Bean
    IssueRateLimitRule verificationKeyCooldownRule() {
        return IssueRateLimitRule.of(
                "verification-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                Duration.ofSeconds(60));
    }

    /**
     * 创建当前应用的每小时签发额度规则。
     *
     * @return 当前应用的每小时签发额度规则
     */
    @Bean
    IssueRateLimitRule applicationHourlyRule() {
        return new GlobalIssueRateLimitRule("application-hour", 1_000, Duration.ofHours(1));
    }

    /**
     * 创建每个邮箱的每小时签发额度规则。
     *
     * @return 每个邮箱的每小时签发额度规则
     */
    @Bean
    IssueRateLimitRule emailSubjectHourlyRule() {
        return IssueRateLimitRule.of(
                "account-email-verification-subject-hour",
                context -> context.key().namespace().equals("account")
                        && context.key().purpose().equals("email-verification"),
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                10,
                Duration.ofHours(1));
    }
}
