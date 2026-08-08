package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 按显式配置创建仅供开发使用的控制台验证码发送器。
 *
 * <p>Creates development-only console verification senders when explicitly
 * configured.</p>
 */
@AutoConfiguration(after = VerificationAutoConfiguration.class)
@ConditionalOnClass(EmailCodeSender.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class VerificationConsoleSenderAutoConfiguration {

    /**
     * 显式启用且用户未提供邮件发送器时创建控制台实现。
     *
     * <p>Creates the console email sender when explicitly enabled and no sender is
     * supplied.</p>
     */
    @Bean
    @ConditionalOnMissingBean(EmailCodeSender.class)
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX + ".email",
            name = "console-enabled",
            havingValue = "true")
    EmailCodeSender consoleEmailCodeSender() {
        return new ConsoleEmailCodeSender();
    }

    /**
     * 显式启用且用户未提供短信发送器时创建控制台实现。
     *
     * <p>Creates the console SMS sender when explicitly enabled and no sender is
     * supplied.</p>
     */
    @Bean
    @ConditionalOnMissingBean(SmsCodeSender.class)
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX + ".sms",
            name = "console-enabled",
            havingValue = "true")
    SmsCodeSender consoleSmsCodeSender() {
        return new ConsoleSmsCodeSender();
    }
}
