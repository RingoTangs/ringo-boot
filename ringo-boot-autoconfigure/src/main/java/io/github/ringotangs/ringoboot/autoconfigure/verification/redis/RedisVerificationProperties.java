package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 验证码状态存储配置。
 *
 * <p>Configuration for Redis verification state storage.</p>
 */
@ConfigurationProperties(RedisVerificationProperties.PREFIX)
public class RedisVerificationProperties {

    /** Redis 验证码配置属性前缀。 / Redis verification configuration property prefix. */
    public static final String PREFIX = VerificationProperties.PREFIX + ".redis";

    /** Base64 编码的共享 HMAC 密钥。 / Base64-encoded shared HMAC secret. */
    private @Nullable String secret;

    /** 业务过期后的记录保留时间。 / Record retention after business expiration. */
    private Duration expiredRetention = Duration.ofMinutes(1);

    public @Nullable String getSecret() {
        return secret;
    }

    public void setSecret(@Nullable String secret) {
        this.secret = secret;
    }

    public Duration getExpiredRetention() {
        return expiredRetention;
    }

    public void setExpiredRetention(Duration expiredRetention) {
        this.expiredRetention = expiredRetention;
    }
}
