package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationStore;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置框架无关的验证码服务组件。
 *
 * <p>Auto-configures the framework-neutral verification service components.</p>
 */
@AutoConfiguration
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationAutoConfiguration {

    /**
     * 在用户未提供生成器时创建安全的数字验证码生成器。
     *
     * <p>Creates a secure numeric code generator when the user has not supplied one.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    CodeGenerator verificationCodeGenerator() {
        return new NumericCodeGenerator();
    }

    /**
     * 根据配置属性创建默认验证码策略。
     *
     * <p>Creates the default verification policy from configuration properties.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    VerificationPolicy verificationPolicy(VerificationProperties properties) {
        return new VerificationPolicy(
                properties.getLength(),
                properties.getTtl(),
                properties.getMaxAttempts(),
                properties.getResendInterval());
    }

    /**
     * 在用户未提供存储时创建内存实现。
     *
     * <p>Creates the in-memory store when the user has not supplied one.</p>
     */
    @Bean
    @ConditionalOnMissingBean(VerificationStore.class)
    VerificationStore inMemoryVerificationStore() {
        return new InMemoryVerificationStore();
    }

    /**
     * 在用户提供邮件发送器时创建邮件验证服务。
     *
     * <p>Creates the email verification service when an email sender is available.</p>
     */
    @Bean
    @ConditionalOnBean(EmailCodeSender.class)
    @ConditionalOnMissingBean(EmailVerificationService.class)
    EmailVerificationService emailVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy policy, EmailCodeSender sender) {
        return new EmailVerificationService(codeGenerator, store, policy, sender);
    }

    /**
     * 在用户提供短信发送器时创建短信验证服务。
     *
     * <p>Creates the SMS verification service when an SMS sender is available.</p>
     */
    @Bean
    @ConditionalOnBean(SmsCodeSender.class)
    @ConditionalOnMissingBean(SmsVerificationService.class)
    SmsVerificationService smsVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy policy, SmsCodeSender sender) {
        return new SmsVerificationService(codeGenerator, store, policy, sender);
    }
}
