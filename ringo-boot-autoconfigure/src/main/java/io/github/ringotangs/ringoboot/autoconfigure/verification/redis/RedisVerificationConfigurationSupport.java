package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import java.util.Base64;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/** Redis 验证码自动配置共享的属性解析与校验工具。 */
final class RedisVerificationConfigurationSupport {

    private RedisVerificationConfigurationSupport() {}

    /** 根据专用配置或 Spring 应用名称解析 Redis key 使用的应用名称。 */
    static String applicationName(RedisVerificationProperties properties, Environment environment) {
        String applicationName = properties.getApplicationName();
        if (applicationName == null || applicationName.isBlank()) {
            applicationName = environment.getProperty("spring.application.name");
        }
        if (applicationName == null || applicationName.isBlank()) {
            throw new IllegalStateException(
                    "ringo.boot.verification.redis.application-name or spring.application.name must be configured");
        }
        return applicationName;
    }

    /** 解码并校验 Redis 验证码能力共享的 Base64 HMAC 密钥。 */
    static byte[] decodeSecret(@Nullable String secret) {
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
