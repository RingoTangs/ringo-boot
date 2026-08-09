package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.generator.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
     * 在用户未提供存储时创建内存实现。
     *
     * <p>Creates the in-memory store when the user has not supplied one.</p>
     */
    @Bean
    @ConditionalOnMissingBean(VerificationStore.class)
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX,
            name = "store",
            havingValue = "memory",
            matchIfMissing = true)
    VerificationStore inMemoryVerificationStore() {
        return new InMemoryVerificationStore();
    }

    /**
     * 当显式选择 Redis 但没有可用实现时快速失败。
     *
     * <p>Fails fast when Redis is explicitly selected but no usable implementation is
     * available.</p>
     */
    @Bean
    @ConditionalOnMissingBean(VerificationStore.class)
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
    VerificationStore missingRedisVerificationStore() {
        throw new IllegalStateException(
                "Redis verification storage requires Spring Data Redis, a StringRedisTemplate, and a valid secret");
    }

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
