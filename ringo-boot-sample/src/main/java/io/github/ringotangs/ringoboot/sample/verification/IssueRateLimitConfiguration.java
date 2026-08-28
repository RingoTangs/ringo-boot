package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.limit.NamespaceIssueQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.PurposeIssueQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.SubjectIssueQuotaRule;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为示例应用配置验证码签发限流规则。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
class IssueRateLimitConfiguration {

    @Bean
    NamespaceIssueQuotaRule accountEmailHourlyQuotaRule() {
        return NamespaceIssueQuotaRule.builder()
                .id("account-email-hourly-quota")
                .namespace("account")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(1_00)
                .window(Duration.ofHours(1L))
                .build();
    }

    @Bean
    PurposeIssueQuotaRule emailVerificationHourlyQuotaRule() {
        return PurposeIssueQuotaRule.builder()
                .id("email-verification-hourly-quota")
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofHours(1L))
                .build();
    }

    @Bean
    SubjectIssueQuotaRule emailVerificationResendCooldownRule() {
        return SubjectIssueQuotaRule.builder()
                .id("email-verification-resend-cooldown")
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(5)
                .window(Duration.ofMinutes(1L))
                .build();
    }
}
