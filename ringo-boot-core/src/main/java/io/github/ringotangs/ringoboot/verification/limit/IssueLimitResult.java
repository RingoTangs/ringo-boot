package io.github.ringotangs.ringoboot.verification.limit;

import java.time.Duration;
import java.util.Objects;

/** 表示验证码签发限流器获取名额的结果。 */
public sealed interface IssueLimitResult permits IssueLimitResult.Allowed, IssueLimitResult.Throttled {

    /** 表示已获得本次验证码签发名额。 */
    record Allowed() implements IssueLimitResult {}

    /**
     * 表示当前请求受限，尚未获得签发名额。
     *
     * @param retryAfter 距离允许再次尝试签发的剩余时间
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
