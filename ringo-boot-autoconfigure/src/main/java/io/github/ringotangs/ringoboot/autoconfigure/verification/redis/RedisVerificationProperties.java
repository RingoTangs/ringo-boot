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

    /**
     * Base64 编码的共享 HMAC 密钥，用于生成 Redis 验证键和验证码的 HMAC-SHA256 摘要，避免在
     * Redis 中保存邮箱、手机号和验证码明文，并降低低熵验证码摘要被离线枚举的风险。
     *
     * <p>Base64-encoded shared HMAC secret used to create HMAC-SHA256 digests for Redis
     * verification keys and codes. It prevents email addresses, phone numbers, and codes from
     * being stored in plaintext and reduces the risk of offline enumeration of low-entropy code
     * digests.</p>
     *
     * <p>Base64 仅用于将二进制密钥表示为配置字符串，不提供额外安全性。该密钥不是 Redis 登录密码，
     * 也不加密 Redis 数据。密钥解码后必须至少为 32 字节，建议使用密码学安全随机数生成器生成，例如
     * {@code openssl rand -base64 32}，不要使用人工编写的密码。</p>
     *
     * <p>Base64 only represents the binary key as a configuration string and provides no additional
     * security. This secret is neither the Redis authentication password nor an encryption key for
     * Redis data. It must decode to at least 32 bytes and should be produced by a cryptographically
     * secure random number generator, for example {@code openssl rand -base64 32}, rather than from
     * a human-chosen password.</p>
     *
     * <p>所有共享同一 Redis 数据的应用实例必须使用相同密钥，应用重启后也必须保持不变。更换密钥会使
     * 尚未过期的验证码失效。应通过环境变量或 Secret Manager 注入，不要提交到源码仓库或输出到日志。</p>
     *
     * <p>All application instances sharing the same Redis data must use the same secret, and the
     * secret must remain stable across restarts. Changing it invalidates unexpired verification
     * codes. Inject it through an environment variable or secret manager; do not commit it to the
     * source repository or write it to logs.</p>
     */
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
