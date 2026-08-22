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
 * 自动配置验证码生成器、状态存储和渠道发送器。
 *
 * <p>仅在显式启用验证码功能时生效。每个默认组件都会在应用提供同类型 Bean 时回退。</p>
 *
 * <p>Auto-configures the verification code generator, state store, and channel senders. It is
 * activated only when verification is explicitly enabled, and each default component backs off
 * when the application provides a bean of the same type.</p>
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
     *
     * @return 安全的数字验证码生成器 / a secure numeric verification code generator
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
     *
     * @return 进程内验证码状态存储 / the in-process verification state store
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
     *
     * @return 此方法不会正常返回 / this method never returns normally
     * @throws IllegalStateException 始终抛出，用于报告 Redis 存储依赖或配置缺失 / always thrown
     *     to report missing Redis storage dependencies or configuration
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
     *
     * @return 仅供本地开发使用的标准输出邮件发送器 / the standard-output email sender for local
     *     development only
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
     *
     * @return 仅供本地开发使用的标准输出短信发送器 / the standard-output SMS sender for local
     *     development only
     */
    @Bean
    @ConditionalOnMissingBean(SmsCodeSender.class)
    SmsCodeSender stdoutSmsCodeSender() {
        return new StdoutSmsCodeSender();
    }
}
