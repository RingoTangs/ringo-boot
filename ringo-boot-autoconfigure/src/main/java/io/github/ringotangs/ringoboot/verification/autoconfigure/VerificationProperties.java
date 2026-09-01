package io.github.ringotangs.ringoboot.verification.autoconfigure;

import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ringo Boot 验证码自动配置属性。
 */
@ConfigurationProperties(VerificationProperties.PREFIX)
public class VerificationProperties {

    /**
     * 配置属性前缀。
     */
    public static final String PREFIX = "ringo.boot.verification";

    /**
     * 是否启用验证码自动配置；默认关闭，需要显式开启。
     */
    private boolean enabled;

    /**
     * 验证码状态存储类型，默认为进程内存储。
     */
    private VerificationStoreType store = VerificationStoreType.MEMORY;

    /**
     * 数字验证码长度，必须大于零，默认为 {@code 6}。
     */
    private int length = 6;

    /**
     * 验证码有效期，必须为正数，默认为五分钟。
     */
    private Duration ttl = Duration.ofMinutes(5);

    /**
     * 单个验证码允许的最大校验尝试次数，必须大于零，默认为 {@code 5}。
     */
    private int maxAttempts = 5;

    /**
     * 返回是否启用验证码自动配置。
     *
     * @return 启用时为 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用验证码自动配置。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回验证码状态存储类型。
     *
     * @return 状态存储类型
     */
    public VerificationStoreType getStore() {
        return store;
    }

    /**
     * 设置验证码状态存储类型。
     *
     * @param store 状态存储类型
     */
    public void setStore(VerificationStoreType store) {
        this.store = store;
    }

    /**
     * 返回数字验证码长度。
     *
     * @return 验证码长度
     */
    public int getLength() {
        return length;
    }

    /**
     * 设置数字验证码长度。
     *
     * @param length 验证码长度，必须大于零
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * 返回验证码有效期。
     *
     * @return 验证码有效期
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * 设置验证码有效期。
     *
     * @param ttl 验证码有效期，必须为正数
     */
    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * 返回单个验证码允许的最大校验尝试次数。
     *
     * @return 最大校验尝试次数
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 设置单个验证码允许的最大校验尝试次数。
     *
     * @param maxAttempts 最大校验尝试次数，必须大于零
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 根据当前配置创建不可变的验证码策略。
     *
     * @return 当前配置对应的验证码策略
     */
    public VerificationPolicy toPolicy() {
        return new VerificationPolicy(length, ttl, maxAttempts);
    }
}
