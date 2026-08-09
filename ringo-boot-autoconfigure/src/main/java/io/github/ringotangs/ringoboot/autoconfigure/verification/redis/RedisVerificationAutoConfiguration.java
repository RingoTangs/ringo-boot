package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.util.Base64;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 显式选择 Redis 时自动配置验证码状态存储。
 *
 * <p>Auto-configures verification state storage when Redis is explicitly selected.</p>
 */
@AutoConfiguration(before = VerificationAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = VerificationProperties.PREFIX, name = "store", havingValue = "redis")
@EnableConfigurationProperties({VerificationProperties.class, RedisVerificationProperties.class})
public class RedisVerificationAutoConfiguration {

    /**
     * 使用 Spring Data Redis 模板和配置的共享密钥创建 Redis Store。
     *
     * <p>Creates the Redis store with the Spring Data Redis template and configured
     * shared secret.</p>
     */
    @Bean
    @ConditionalOnMissingBean(VerificationStore.class)
    VerificationStore redisVerificationStore(
            StringRedisTemplate redisTemplate, RedisVerificationProperties properties) {
        return new RedisVerificationStore(
                redisTemplate, decodeSecret(properties.getSecret()), properties.getExpiredRetention());
    }

    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("ringo.boot.verification.redis.secret must be configured");
        }
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ringo.boot.verification.redis.secret must be valid Base64", exception);
        }
    }
}
