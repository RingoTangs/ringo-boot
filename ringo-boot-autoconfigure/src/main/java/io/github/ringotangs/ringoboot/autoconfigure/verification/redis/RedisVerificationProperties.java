package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 验证码状态存储的自动配置属性。
 *
 * <p>Auto-configuration properties for Redis verification state storage.</p>
 */
@ConfigurationProperties(RedisVerificationProperties.PREFIX)
public class RedisVerificationProperties {

    /** Redis 验证码配置属性前缀。 / Redis verification configuration property prefix. */
    public static final String PREFIX = VerificationProperties.PREFIX + ".redis";

    /**
     * Redis key 使用的应用名称，用于隔离共享 Redis 实例中的不同应用。未配置时使用
     * {@code spring.application.name}。
     *
     * <p>Application name included in Redis keys to isolate applications sharing a Redis instance.
     * Falls back to {@code spring.application.name} when not configured.</p>
     *
     * <p>名称必须以字母或数字开头，并且只能包含字母、数字、点、下划线和连字符。修改名称会使旧名称下
     * 尚未过期的验证码不可访问。</p>
     *
     * <p>The name must start with a letter or digit and contain only letters, digits, dots,
     * underscores, and hyphens. Changing it makes unexpired codes under the old name
     * inaccessible.</p>
     */
    private @Nullable String applicationName;

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

    /**
     * 验证码业务过期后 Redis 记录继续保留的时间，必须为正数，默认为一分钟。
     *
     * <p>How long the Redis record remains after the verification code expires for business use.
     * Must be positive and defaults to one minute.</p>
     */
    private Duration expiredRetention = Duration.ofMinutes(1);

    /**
     * 返回 Redis key 使用的应用名称覆盖值。
     *
     * <p>Returns the application-name override used in Redis keys.</p>
     *
     * @return 应用名称覆盖值，未配置时为 {@code null} / application-name override, or
     *     {@code null} when absent
     */
    public @Nullable String getApplicationName() {
        return applicationName;
    }

    /**
     * 设置 Redis key 使用的应用名称覆盖值。
     *
     * <p>Sets the application-name override used in Redis keys.</p>
     *
     * @param applicationName 应用名称覆盖值 / application-name override
     */
    public void setApplicationName(@Nullable String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 返回 Base64 编码的共享 HMAC 密钥。
     *
     * <p>Returns the Base64-encoded shared HMAC secret.</p>
     *
     * @return 共享密钥，未配置时为 {@code null} / the shared secret, or {@code null} when not
     *     configured
     */
    public @Nullable String getSecret() {
        return secret;
    }

    /**
     * 设置 Base64 编码的共享 HMAC 密钥。
     *
     * <p>Sets the Base64-encoded shared HMAC secret.</p>
     *
     * @param secret 共享密钥，解码后必须至少为 32 字节 / the shared secret, which must decode to
     *     at least 32 bytes
     */
    public void setSecret(@Nullable String secret) {
        this.secret = secret;
    }

    /**
     * 返回验证码业务过期后的 Redis 记录保留时间。
     *
     * <p>Returns the Redis record retention after business expiration.</p>
     *
     * @return 过期记录保留时间 / the expired record retention
     */
    public Duration getExpiredRetention() {
        return expiredRetention;
    }

    /**
     * 设置验证码业务过期后的 Redis 记录保留时间。
     *
     * <p>Sets the Redis record retention after business expiration.</p>
     *
     * @param expiredRetention 过期记录保留时间，必须为正数 / the expired record retention,
     *     which must be positive
     */
    public void setExpiredRetention(Duration expiredRetention) {
        this.expiredRetention = expiredRetention;
    }
}
