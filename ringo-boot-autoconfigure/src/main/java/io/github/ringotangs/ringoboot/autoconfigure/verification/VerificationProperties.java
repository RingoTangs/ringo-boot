package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
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

    /**
     * 是否启用验证码自动配置；默认关闭，需要显式开启。
     *
     * <p>Whether verification auto-configuration is enabled. It is disabled by default and requires
     * explicit opt-in.</p>
     */
    private boolean enabled;

    /**
     * 验证码状态存储类型，默认为进程内存储。
     *
     * <p>Verification state storage type. Defaults to in-process storage.</p>
     */
    private VerificationStoreType store = VerificationStoreType.MEMORY;

    /**
     * 数字验证码长度，必须大于零，默认为 {@code 6}。
     *
     * <p>Length of generated numeric verification codes. Must be greater than zero and defaults to
     * {@code 6}.</p>
     */
    private int length = 6;

    /**
     * 验证码有效期，必须为正数，默认为五分钟。
     *
     * <p>Verification code time to live. Must be positive and defaults to five minutes.</p>
     */
    private Duration ttl = Duration.ofMinutes(5);

    /**
     * 单个验证码允许的最大校验尝试次数，必须大于零，默认为 {@code 5}。
     *
     * <p>Maximum number of verification attempts allowed for one code. Must be greater than zero
     * and defaults to {@code 5}.</p>
     */
    private int maxAttempts = 5;

    /**
     * 返回是否启用验证码自动配置。
     *
     * <p>Returns whether verification auto-configuration is enabled.</p>
     *
     * @return 启用时为 {@code true} / {@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用验证码自动配置。
     *
     * <p>Sets whether verification auto-configuration is enabled.</p>
     *
     * @param enabled 是否启用 / whether to enable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回验证码状态存储类型。
     *
     * <p>Returns the verification state storage type.</p>
     *
     * @return 状态存储类型 / the state storage type
     */
    public VerificationStoreType getStore() {
        return store;
    }

    /**
     * 设置验证码状态存储类型。
     *
     * <p>Sets the verification state storage type.</p>
     *
     * @param store 状态存储类型 / the state storage type
     */
    public void setStore(VerificationStoreType store) {
        this.store = store;
    }

    /**
     * 返回数字验证码长度。
     *
     * <p>Returns the numeric verification code length.</p>
     *
     * @return 验证码长度 / the verification code length
     */
    public int getLength() {
        return length;
    }

    /**
     * 设置数字验证码长度。
     *
     * <p>Sets the numeric verification code length.</p>
     *
     * @param length 验证码长度，必须大于零 / the code length, which must be greater than zero
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * 返回验证码有效期。
     *
     * <p>Returns the verification code time to live.</p>
     *
     * @return 验证码有效期 / the verification code time to live
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * 设置验证码有效期。
     *
     * <p>Sets the verification code time to live.</p>
     *
     * @param ttl 验证码有效期，必须为正数 / the time to live, which must be positive
     */
    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * 返回单个验证码允许的最大校验尝试次数。
     *
     * <p>Returns the maximum number of verification attempts allowed for one code.</p>
     *
     * @return 最大校验尝试次数 / the maximum number of verification attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 设置单个验证码允许的最大校验尝试次数。
     *
     * <p>Sets the maximum number of verification attempts allowed for one code.</p>
     *
     * @param maxAttempts 最大校验尝试次数，必须大于零 / the maximum number of attempts, which
     *     must be greater than zero
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 根据当前配置创建不可变的验证码策略。
     *
     * <p>Creates an immutable verification policy from the current configuration.</p>
     *
     * @return 当前配置对应的验证码策略 / the verification policy represented by this configuration
     */
    VerificationPolicy toPolicy() {
        return new VerificationPolicy(length, ttl, maxAttempts);
    }
}
