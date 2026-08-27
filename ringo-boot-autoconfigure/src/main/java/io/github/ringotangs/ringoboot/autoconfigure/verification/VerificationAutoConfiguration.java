package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisVerificationStore;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.generator.NumericCodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationService;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 自动配置验证码生成器、状态存储、渠道发送器和验证服务。
 *
 * <p>仅在显式启用验证码功能时生效。每个默认组件都会在应用提供同类型 Bean 时回退。</p>
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, IssueRateLimitAutoConfiguration.class})
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationAutoConfiguration {

    /**
     * 在用户未提供生成器时创建安全的数字验证码生成器。
     *
     * @return 安全的数字验证码生成器
     */
    @Bean
    @ConditionalOnMissingBean
    CodeGenerator numericCodeGenerator() {
        return new NumericCodeGenerator();
    }

    /**
     * 在用户未提供存储时创建内存实现。
     *
     * @return 进程内验证码状态存储
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
     * 当显式选择 Redis 但没有可用操作模板时快速失败。
     *
     * @return 此方法不会正常返回
     * @throws IllegalStateException 始终抛出，用于报告 Redis 存储依赖或配置缺失
     */
    @Bean
    @ConditionalOnMissingBean(
            value = VerificationStore.class,
            type = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
    VerificationStore unavailableRedisVerificationStore() {
        throw new IllegalStateException(
                "Redis verification storage requires Spring Data Redis and a StringRedisTemplate");
    }

    /**
     * 在用户未提供邮件发送器时创建标准输出实现。
     *
     * @return 仅供本地开发使用的标准输出邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean(EmailCodeSender.class)
    EmailCodeSender stdoutEmailCodeSender() {
        return new StdoutEmailCodeSender();
    }

    /**
     * 在用户未提供短信发送器时创建标准输出实现。
     *
     * @return 仅供本地开发使用的标准输出短信发送器
     */
    @Bean
    @ConditionalOnMissingBean(SmsCodeSender.class)
    SmsCodeSender stdoutSmsCodeSender() {
        return new StdoutSmsCodeSender();
    }

    /**
     * 在邮件发送器、唯一状态存储和唯一签发限流器可用时创建邮件验证服务。
     *
     * @param codeGenerator    验证码生成器
     * @param store            验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param properties       验证码配置属性
     * @param sender           邮件验证码发送器
     * @return 邮件验证码服务
     */
    @Bean
    @ConditionalOnBean(EmailCodeSender.class)
    @Conditional(OnVerificationServiceDependenciesCondition.class)
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
     * 在短信发送器、唯一状态存储和唯一签发限流器可用时创建短信验证服务。
     *
     * @param codeGenerator    验证码生成器
     * @param store            验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param properties       验证码配置属性
     * @param sender           短信验证码发送器
     * @return 短信验证码服务
     */
    @Bean
    @ConditionalOnBean(SmsCodeSender.class)
    @Conditional(OnVerificationServiceDependenciesCondition.class)
    @ConditionalOnMissingBean(SmsVerificationService.class)
    SmsVerificationService smsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationProperties properties,
            SmsCodeSender sender) {
        return new SmsVerificationService(codeGenerator, store, issueRateLimiter, properties.toPolicy(), sender);
    }

    /**
     * Redis 验证码状态存储配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
    static class RedisStoreConfiguration {

        private static final Duration EXPIRED_RETENTION = Duration.ofMinutes(1);

        /**
         * 使用 Redis 操作模板和共享密钥创建验证码状态存储。
         *
         * @param redisTemplate Redis 字符串操作模板
         * @param hmacKey       应用提供的验证码 HMAC 密钥
         * @param environment   Spring 环境，用于读取应用名称
         * @return Redis 验证码状态存储
         * @throws IllegalStateException 当共享密钥或应用名称无效时
         */
        @Bean
        @ConditionalOnMissingBean(VerificationStore.class)
        VerificationStore redisVerificationStore(
                StringRedisTemplate redisTemplate, VerificationHmacKey hmacKey, Environment environment) {
            return new RedisVerificationStore(
                    redisTemplate,
                    hmacKey.getEncoded(),
                    EXPIRED_RETENTION,
                    environment.getRequiredProperty("spring.application.name"));
        }
    }

    /**
     * 同时检查验证码状态存储和签发限流器是否存在唯一候选 Bean。
     */
    static final class OnVerificationServiceDependenciesCondition extends AllNestedConditions {

        OnVerificationServiceDependenciesCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnSingleCandidate(VerificationStore.class)
        static class SingleVerificationStore {}

        @ConditionalOnSingleCandidate(IssueRateLimiter.class)
        static class SingleIssueRateLimiter {}
    }
}
