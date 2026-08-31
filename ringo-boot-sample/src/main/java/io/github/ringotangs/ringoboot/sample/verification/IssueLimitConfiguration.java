package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.limit.NamespaceQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.PurposeQuotaRule;
import io.github.ringotangs.ringoboot.verification.limit.SubjectQuotaRule;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为示例应用配置验证码签发限流规则。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
class IssueLimitConfiguration {

    @Bean
    NamespaceQuotaRule accountEmailHourlyQuotaRule() {
        return NamespaceQuotaRule.builder()
                .namespace("account")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(1_00)
                .window(Duration.ofHours(1L))
                .build();
    }

    @Bean
    PurposeQuotaRule emailVerificationHourlyQuotaRule() {
        return PurposeQuotaRule.builder()
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(10)
                .window(Duration.ofHours(1L))
                .build();
    }

    @Bean
    SubjectQuotaRule emailVerificationResendCooldownRule() {
        return SubjectQuotaRule.builder()
                .namespace("account")
                .purpose("email-verification")
                .channel(VerificationChannel.EMAIL)
                .maxIssues(5)
                .window(Duration.ofMinutes(1L))
                .build();
    }
}
