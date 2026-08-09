package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
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
 *
 * <p>Auto-configures email and SMS verification services when a generator, state store, and the
 * corresponding sender port are available. Each channel service backs off when an application bean
 * of the same type exists, and both services share the global verification policy.</p>
 */
@AutoConfiguration(after = VerificationAutoConfiguration.class)
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnSingleCandidate(VerificationStore.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationChannelAutoConfiguration {

    /**
     * 在邮件发送器可用时创建邮件验证服务。
     *
     * <p>Creates the email verification service when an email sender is available.</p>
     *
     * @param codeGenerator 验证码生成器 / the verification code generator
     * @param store 验证码状态存储 / the verification state store
     * @param properties 验证码配置属性 / the verification configuration properties
     * @param sender 邮件验证码发送器 / the email verification code sender
     * @return 邮件验证码服务 / the email verification service
     */
    @Bean
    @ConditionalOnBean(EmailCodeSender.class)
    @ConditionalOnMissingBean(EmailVerificationService.class)
    EmailVerificationService emailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            VerificationProperties properties,
            EmailCodeSender sender) {
        return new EmailVerificationService(codeGenerator, store, properties.toPolicy(), sender);
    }

    /**
     * 在短信发送器可用时创建短信验证服务。
     *
     * <p>Creates the SMS verification service when an SMS sender is available.</p>
     *
     * @param codeGenerator 验证码生成器 / the verification code generator
     * @param store 验证码状态存储 / the verification state store
     * @param properties 验证码配置属性 / the verification configuration properties
     * @param sender 短信验证码发送器 / the SMS verification code sender
     * @return 短信验证码服务 / the SMS verification service
     */
    @Bean
    @ConditionalOnBean(SmsCodeSender.class)
    @ConditionalOnMissingBean(SmsVerificationService.class)
    SmsVerificationService smsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            VerificationProperties properties,
            SmsCodeSender sender) {
        return new SmsVerificationService(codeGenerator, store, properties.toPolicy(), sender);
    }
}
