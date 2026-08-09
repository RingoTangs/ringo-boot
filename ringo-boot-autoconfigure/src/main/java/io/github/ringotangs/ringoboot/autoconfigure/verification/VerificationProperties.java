package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ringo Boot 验证码自动配置属性。
 *
 * <p>Auto-configuration properties for Ringo Boot verification services.</p>
 */
@ConfigurationProperties(VerificationProperties.PREFIX)
public class VerificationProperties {

    /** 配置属性前缀。 / Configuration property prefix. */
    public static final String PREFIX = "ringo.boot.verification";

    /** 是否启用验证码自动配置。 / Whether verification auto-configuration is enabled. */
    private boolean enabled;

    /** 验证码状态存储类型。 / Verification state storage type. */
    private VerificationStoreType store = VerificationStoreType.MEMORY;

    /** 默认验证码长度。 / Default verification code length. */
    private int length = 6;

    /** 默认验证码有效期。 / Default verification code time to live. */
    private Duration ttl = Duration.ofMinutes(5);

    /** 默认最大校验尝试次数。 / Default maximum number of verification attempts. */
    private int maxAttempts = 5;

    /** 默认重发间隔。 / Default resend interval. */
    private Duration resendInterval = Duration.ofSeconds(60);

    /** Redis 存储配置。 / Redis storage configuration. */
    private final Redis redis = new Redis();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public VerificationStoreType getStore() {
        return store;
    }

    public void setStore(VerificationStoreType store) {
        this.store = store;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    public Redis getRedis() {
        return redis;
    }

    VerificationPolicy toPolicy() {
        return new VerificationPolicy(length, ttl, maxAttempts, resendInterval);
    }

    /** Redis 验证码存储配置。 / Redis verification storage configuration. */
    public static class Redis {

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
}
