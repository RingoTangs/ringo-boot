package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示验证码签发及交付流程的安全结果，不包含明文验证码。
 *
 */
public sealed interface IssueResult permits IssueResult.Accepted, IssueResult.Uncertain, IssueResult.Throttled {

    /**
     * 表示验证码已成功签发，并且发送供应商明确接受了请求。
     *
     *
     * @param expiresAt 验证码过期时间
     */
    record Accepted(Instant expiresAt) implements IssueResult {

        /**
         * 创建并校验渠道已受理的签发结果。
         *
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时
         */
        public Accepted {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码已成功签发，但无法确认发送供应商是否接受请求。
     *
     *
     * @param expiresAt 验证码过期时间
     */
    record Uncertain(Instant expiresAt) implements IssueResult {

        /** 创建并校验渠道受理状态不确定的签发结果。 */
        public Uncertain {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码因签发频率限制而未签发或交付。
     *
     *
     * @param retryAfter 距离允许再次签发的剩余时间
     */
    record Throttled(Duration retryAfter) implements IssueResult {

        /**
         * 创建并校验受限流的签发结果。
         *
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
