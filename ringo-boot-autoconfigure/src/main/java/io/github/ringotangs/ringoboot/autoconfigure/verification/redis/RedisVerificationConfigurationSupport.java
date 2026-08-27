package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

/**
 * Redis 验证码自动配置共享的属性解析与校验工具。
 */
final class RedisVerificationConfigurationSupport {

    private RedisVerificationConfigurationSupport() {}

    /**
     * 根据 Spring 应用名称解析 Redis key 使用的应用名称。
     */
    static String applicationName(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name");
        if (applicationName == null || applicationName.isBlank()) {
            throw new IllegalStateException(
                    "spring.application.name must be configured for Redis verification storage");
        }
        return applicationName;
    }

    /**
     * 获取应用提供的唯一 Redis 验证码 HMAC 密钥。
     */
    static RedisVerificationHmacKey hmacKey(ObjectProvider<RedisVerificationHmacKey> keys) {
        RedisVerificationHmacKey key = keys.getIfUnique();
        if (key == null) {
            throw new IllegalStateException("exactly one RedisVerificationHmacKey bean must be configured");
        }
        return key;
    }
}
