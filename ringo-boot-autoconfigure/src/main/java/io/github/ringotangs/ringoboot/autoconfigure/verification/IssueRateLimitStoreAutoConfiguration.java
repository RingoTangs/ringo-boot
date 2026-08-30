package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.verification.redis.RedisIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 在签发限流入口装配前配置其状态存储。
 *
 * <p>只有应用声明限流规则且没有替换整个限流器或状态存储时才创建默认 Store。
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(IssueRateLimiter.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
public class IssueRateLimitStoreAutoConfiguration {

    /**
     * 在应用提供规则、选择内存模式且未提供存储时创建进程内签发限流状态存储。
     *
     * @return 进程内验证码签发限流状态存储
     */
    @Bean
    @ConditionalOnBean(IssueRateLimitRule.class)
    @ConditionalOnMissingBean({IssueRateLimiter.class, IssueRateLimitStore.class})
    @ConditionalOnProperty(
            prefix = VerificationProperties.PREFIX,
            name = "store",
            havingValue = "memory",
            matchIfMissing = true)
    IssueRateLimitStore inMemoryIssueRateLimitStore() {
        return new InMemoryIssueRateLimitStore();
    }

    /** 在显式选择 Redis 且应用提供限流规则时配置跨实例限流状态存储。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean({StringRedisTemplate.class, IssueRateLimitRule.class})
    @ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
    static class RedisStoreConfiguration {

        /**
         * 使用 Redis、共享 HMAC 密钥和应用名称创建签发限流状态存储。
         *
         * @param redisTemplate Redis 字符串操作模板
         * @param hmacKey       应用提供的验证码 HMAC 密钥
         * @param environment   Spring 环境
         * @return Redis 验证码签发限流状态存储
         */
        @Bean
        @ConditionalOnMissingBean({IssueRateLimiter.class, IssueRateLimitStore.class})
        IssueRateLimitStore redisIssueRateLimitStore(
                StringRedisTemplate redisTemplate, VerificationHmacKey hmacKey, Environment environment) {
            return new RedisIssueRateLimitStore(
                    redisTemplate, hmacKey.getEncoded(), environment.getRequiredProperty("spring.application.name"));
        }
    }
}
