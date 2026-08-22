package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationAutoConfiguration;
import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 显式选择 Redis 且存在 {@link StringRedisTemplate} 时自动配置验证码状态存储。
 *
 * <p>用户提供自定义 {@link VerificationStore} 时，默认 Redis 实现自动回退。</p>
 *
 * <p>Auto-configures verification state storage when Redis is explicitly selected and a
 * {@link StringRedisTemplate} is available. The default Redis implementation backs off when the
 * application provides a custom {@link VerificationStore}.</p>
 */
@AutoConfiguration(after = RedisAutoConfiguration.class, before = VerificationAutoConfiguration.class)
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
     *
     * @param redisTemplate Redis 字符串操作模板 / the Redis string operations template
     * @param properties Redis 验证码存储配置 / the Redis verification storage configuration
     * @param environment Spring 环境，用于读取后备应用名称 / Spring environment used to read
     *     the fallback application name
     * @return Redis 验证码状态存储 / the Redis verification state store
     * @throws IllegalStateException 当共享密钥或应用名称缺失，或密钥不是有效 Base64 时 / if the
     *     shared secret or application name is missing, or the secret is not valid Base64
     */
    @Bean
    @ConditionalOnMissingBean(VerificationStore.class)
    VerificationStore redisVerificationStore(
            StringRedisTemplate redisTemplate, RedisVerificationProperties properties, Environment environment) {
        return new RedisVerificationStore(
                redisTemplate,
                RedisVerificationConfigurationSupport.decodeSecret(properties.getSecret()),
                properties.getExpiredRetention(),
                RedisVerificationConfigurationSupport.applicationName(properties, environment));
    }
}
