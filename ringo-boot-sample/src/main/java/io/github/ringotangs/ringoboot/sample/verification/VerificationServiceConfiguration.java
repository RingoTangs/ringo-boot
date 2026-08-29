package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.generator.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式组装示例应用使用的验证码渠道服务。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
class VerificationServiceConfiguration {

    @Bean
    EmailVerificationService emailVerificationService(
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationProperties properties,
            EmailCodeSender sender) {
        return new EmailVerificationService(
                new NumericCodeGenerator(), store, issueRateLimiter, properties.toPolicy(), sender);
    }
}
