package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.util.Objects;

/**
 * 定义验证码生成及校验生命周期的限制策略。
 *
 * @param length      验证码长度
 * @param ttl         验证码有效期
 * @param maxAttempts 最大校验尝试次数
 */
public record VerificationPolicy(int length, Duration ttl, int maxAttempts) {

    private static final int DEFAULT_LENGTH = 6;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 创建并校验验证码策略。
     *
     * @throws NullPointerException     当有效期为 {@code null} 时
     * @throws IllegalArgumentException 当长度、有效期或尝试次数非法时
     */
    public VerificationPolicy {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0: " + length);
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than 0: " + maxAttempts);
        }
    }

    /**
     * 返回 {@link VerificationService#issue(VerificationKey)} 使用的安全默认策略。
     * 默认生成 6 位验证码，有效期 5 分钟，最多尝试 5 次。
     *
     * @return 默认验证码策略
     */
    public static VerificationPolicy defaults() {
        return new VerificationPolicy(DEFAULT_LENGTH, DEFAULT_TTL, DEFAULT_MAX_ATTEMPTS);
    }
}
