package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.autoconfigure.verification.VerificationProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Redis 验证码状态存储和签发限流的共享配置。 */
@ConfigurationProperties(RedisVerificationProperties.PREFIX)
public class RedisVerificationProperties {

    /** Redis 验证码配置属性前缀。 */
    public static final String PREFIX = VerificationProperties.PREFIX + ".redis";

    /**
     * Redis key 使用的应用名称，用于隔离共享 Redis 实例中的不同应用。未配置时使用
     * {@code spring.application.name}。
     *
     * <p>名称必须以字母或数字开头，并且只能包含字母、数字、点、下划线和连字符。修改名称会使旧名称下
     * 尚未过期的验证码不可访问。</p>
     */
    private @Nullable String applicationName;

    /**
     * Base64 编码的共享 HMAC 密钥，用于生成 Redis 验证键和验证码的 HMAC-SHA256 摘要，避免在
     * Redis 中保存邮箱、手机号和验证码明文，并降低低熵验证码摘要被离线枚举的风险。
     *
     * <p>Base64 仅用于将二进制密钥表示为配置字符串，不提供额外安全性。该密钥不是 Redis 登录密码，
     * 也不加密 Redis 数据。密钥解码后必须至少为 32 字节，建议使用密码学安全随机数生成器生成，例如
     * {@code openssl rand -base64 32}，不要使用人工编写的密码。</p>
     *
     * <p>所有共享同一 Redis 数据的应用实例必须使用相同密钥，应用重启后也必须保持不变。更换密钥会使
     * 尚未过期的验证码失效。应通过环境变量或 Secret Manager 注入，不要提交到源码仓库或输出到日志。</p>
     */
    private @Nullable String secret;

    /**
     * 验证码业务过期后 Redis 记录继续保留的时间，必须为正数，默认为一分钟。
     */
    private Duration expiredRetention = Duration.ofMinutes(1);

    /**
     * 返回 Redis key 使用的应用名称覆盖值。
     *
     * @return 应用名称覆盖值，未配置时为 {@code null}
     */
    public @Nullable String getApplicationName() {
        return applicationName;
    }

    /**
     * 设置 Redis key 使用的应用名称覆盖值。
     *
     * @param applicationName 应用名称覆盖值
     */
    public void setApplicationName(@Nullable String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 返回 Base64 编码的共享 HMAC 密钥。
     *
     * @return 共享密钥，未配置时为 {@code null}
     */
    public @Nullable String getSecret() {
        return secret;
    }

    /**
     * 设置 Base64 编码的共享 HMAC 密钥。
     *
     * @param secret 共享密钥，解码后必须至少为 32 字节
     */
    public void setSecret(@Nullable String secret) {
        this.secret = secret;
    }

    /**
     * 返回验证码业务过期后的 Redis 记录保留时间。
     *
     * @return 过期记录保留时间
     */
    public Duration getExpiredRetention() {
        return expiredRetention;
    }

    /**
     * 设置验证码业务过期后的 Redis 记录保留时间。
     *
     * @param expiredRetention 过期记录保留时间，必须为正数
     */
    public void setExpiredRetention(Duration expiredRetention) {
        this.expiredRetention = expiredRetention;
    }
}
