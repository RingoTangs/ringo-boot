package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/** 表示 {@link IssueRateLimiter} 尝试获取验证码签发名额的结果。 */
public sealed interface IssueLimitResult permits IssueLimitResult.Allowed, IssueLimitResult.Throttled {

    /** 表示已获得并消费本次验证码签发名额。 */
    record Allowed() implements IssueLimitResult {}

    /**
     * 表示当前请求受限，尚未获得签发名额。
     *
     * <p>该结果不表示永久拒绝。调用方可以将 {@code retryAfter} 转换为 HTTP {@code Retry-After} 或其他客户端可理解的等待提示。
     *
     * @param retryAfter 距离全部受限规则再次允许签发的剩余时间
     */
    record Throttled(Duration retryAfter) implements IssueLimitResult {

        /**
         * 创建并校验限流结果。
         *
         * @throws NullPointerException 当剩余时间为 {@code null} 时
         * @throws IllegalArgumentException 当剩余时间为负数时
         */
        public Throttled {
            Objects.requireNonNull(retryAfter, "retryAfter must not be null");
            if (retryAfter.isNegative()) {
                throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
            }
        }
    }
}
