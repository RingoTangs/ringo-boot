package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 为验证码渠道自动配置标准输出发送器。
 *
 * <p>Auto-configures standard-output senders for verification channels.</p>
 */
@AutoConfiguration(after = VerificationAutoConfiguration.class)
@ConditionalOnClass(EmailCodeSender.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationSenderAutoConfiguration {

    /**
     * 在用户未提供邮件发送器时创建标准输出实现。
     *
     * <p>Creates the standard-output email sender when no user sender is supplied.</p>
     */
    @Bean
    @ConditionalOnMissingBean(EmailCodeSender.class)
    EmailCodeSender stdoutEmailCodeSender() {
        return new StdoutEmailCodeSender();
    }

    /**
     * 在用户未提供短信发送器时创建标准输出实现。
     *
     * <p>Creates the standard-output SMS sender when no user sender is supplied.</p>
     */
    @Bean
    @ConditionalOnMissingBean(SmsCodeSender.class)
    SmsCodeSender stdoutSmsCodeSender() {
        return new StdoutSmsCodeSender();
    }
}
