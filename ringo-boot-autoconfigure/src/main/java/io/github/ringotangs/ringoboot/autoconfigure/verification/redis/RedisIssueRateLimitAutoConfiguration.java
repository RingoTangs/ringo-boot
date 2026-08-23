package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.IssueRateLimitAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 显式选择 Redis 时自动配置跨实例验证码签发限流状态存储。
 *
 * <p>仅在 Spring Data Redis 和 {@link StringRedisTemplate} 可用时生效；应用提供自定义 Store 或 Limiter 后，默认 Redis Store
 * 自动回退。</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class, before = IssueRateLimitAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
public class RedisIssueRateLimitAutoConfiguration {

    /**
     * 使用 Redis 和共享 HMAC 密钥创建跨实例签发限流状态存储。
     *
     * @param redisTemplate Redis 字符串操作模板
     * @param hmacKeys 应用提供的 Redis 验证码 HMAC 密钥
     * @param environment Spring 环境
     * @return Redis 验证码签发限流状态存储
     */
    @Bean
    @ConditionalOnMissingBean({IssueRateLimiter.class, IssueRateLimitStore.class})
    IssueRateLimitStore redisIssueRateLimitStore(
            StringRedisTemplate redisTemplate,
            ObjectProvider<RedisVerificationHmacKey> hmacKeys,
            Environment environment) {
        RedisVerificationHmacKey hmacKey = RedisVerificationConfigurationSupport.hmacKey(hmacKeys);
        return new RedisIssueRateLimitStore(
                redisTemplate,
                hmacKey.getEncoded(),
                RedisVerificationConfigurationSupport.applicationName(environment));
    }
}
