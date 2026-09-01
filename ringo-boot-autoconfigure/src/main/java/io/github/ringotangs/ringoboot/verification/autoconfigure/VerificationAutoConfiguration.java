package io.github.ringotangs.ringoboot.verification.autoconfigure;

import io.github.ringotangs.ringoboot.verification.DefaultIssueContextManager;
import io.github.ringotangs.ringoboot.verification.IssueContextContributor;
import io.github.ringotangs.ringoboot.verification.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.email.EmailCodeSender;
import io.github.ringotangs.ringoboot.verification.email.StdoutEmailCodeSender;
import io.github.ringotangs.ringoboot.verification.redis.RedisVerificationStore;
import io.github.ringotangs.ringoboot.verification.redis.VerificationHmacKey;
import io.github.ringotangs.ringoboot.verification.servlet.ClientIpContributor;
import io.github.ringotangs.ringoboot.verification.sms.SmsCodeSender;
import io.github.ringotangs.ringoboot.verification.sms.StdoutSmsCodeSender;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 自动配置验证码状态存储、默认渠道发送器和可选的 Servlet 请求上下文贡献器。
 *
 * <p>仅在显式启用验证码功能时生效。验证服务和验证码生成器由应用显式组装；每个默认基础设施组件都会在应用提供同类型 Bean 时回退。</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(VerificationStore.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VerificationProperties.class)
public class VerificationAutoConfiguration {

    /**
     * 在用户未提供 Manager 时，按 Spring 顺序收集所有上下文 Contributor。
     *
     * @param contributors 应用上下文中的上下文贡献器
     * @return 默认上下文 Manager
     */
    @Bean
    @ConditionalOnMissingBean(IssueContextManager.class)
    IssueContextManager issueContextManager(ObjectProvider<IssueContextContributor> contributors) {
        return new DefaultIssueContextManager(contributors.orderedStream().toList());
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
     * Servlet 请求上下文贡献器配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(HttpServletRequest.class)
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX + ".contributor",
            name = "client-ip",
            havingValue = "true")
    static class ServletContextConfiguration {

        /**
         * 在用户未提供客户端 IP Contributor 时创建默认实现。
         *
         * @param requests 按调用解析当前 Servlet 请求的 Provider
         * @return 客户端 IP 上下文贡献器
         */
        @Bean
        @ConditionalOnMissingBean(ClientIpContributor.class)
        ClientIpContributor clientIpContributor(ObjectProvider<HttpServletRequest> requests) {
            return new ClientIpContributor(requests);
        }
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
}
