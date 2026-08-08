package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.CodeGenerator;
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
import org.springframework.context.annotation.Bean;

/**
 * 在对应发送端口可用时自动配置邮件和短信验证服务。
 *
 * <p>Auto-configures email and SMS verification services when their sender ports are
 * available.</p>
 */
@AutoConfiguration(after = {VerificationAutoConfiguration.class, VerificationConsoleSenderAutoConfiguration.class})
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationChannelAutoConfiguration {

    /**
     * 在邮件发送器可用时创建邮件验证服务。
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
     * 在短信发送器可用时创建短信验证服务。
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
