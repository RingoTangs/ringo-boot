package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.autoconfigure.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.channel.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.channel.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.context.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.generator.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
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
            IssueLimiter issueLimiter,
            VerificationProperties properties,
            IssueContextManager issueContextManager,
            EmailCodeSender sender) {
        return new EmailVerificationService(
                new NumericCodeGenerator(), store, issueLimiter, properties.toPolicy(), issueContextManager, sender);
    }
}
