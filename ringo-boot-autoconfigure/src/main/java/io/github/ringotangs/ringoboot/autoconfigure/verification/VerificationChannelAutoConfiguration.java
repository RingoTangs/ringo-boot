package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationService;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;

/**
 * 在生成器、状态存储和对应发送端口可用时自动配置邮件和短信验证服务。
 *
 * <p>每个渠道服务都会在应用提供同类型 Bean 时回退，并共享全局验证码策略。</p>
 */
@AutoConfiguration(after = {VerificationAutoConfiguration.class, IssueRateLimitAutoConfiguration.class})
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnSingleCandidate(IssueRateLimiter.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationChannelAutoConfiguration {

    /**
     * 在邮件发送器可用时创建邮件验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param properties 验证码配置属性
     * @param sender 邮件验证码发送器
     * @return 邮件验证码服务
     */
    @Bean
    @ConditionalOnBean(EmailCodeSender.class)
    @ConditionalOnSingleCandidate(VerificationStore.class)
    @ConditionalOnMissingBean(EmailVerificationService.class)
    EmailVerificationService emailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationProperties properties,
            EmailCodeSender sender) {
        return new EmailVerificationService(codeGenerator, store, issueRateLimiter, properties.toPolicy(), sender);
    }

    /**
     * 在短信发送器可用时创建短信验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param properties 验证码配置属性
     * @param sender 短信验证码发送器
     * @return 短信验证码服务
     */
    @Bean
    @ConditionalOnBean(SmsCodeSender.class)
    @ConditionalOnSingleCandidate(VerificationStore.class)
    @ConditionalOnMissingBean(SmsVerificationService.class)
    SmsVerificationService smsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationProperties properties,
            SmsCodeSender sender) {
        return new SmsVerificationService(codeGenerator, store, issueRateLimiter, properties.toPolicy(), sender);
    }
}
