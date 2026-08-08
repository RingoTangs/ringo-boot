package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示验证码签发操作的结果。
 *
 * <p>Represents the result of issuing a verification code.</p>
 */
public sealed interface IssueResult permits IssueResult.Issued, IssueResult.Throttled {

    /**
     * 表示验证码已成功签发。调用方应立即交付并尽快丢弃明文验证码。
     *
     * <p>Indicates that a code was issued successfully. Callers should deliver and
     * discard the plaintext code promptly.</p>
     *
     * @param code 新签发的明文验证码 / the newly issued plaintext code
     * @param expiresAt 验证码过期时间 / the instant at which the code expires
     */
    record Issued(String code, Instant expiresAt) implements IssueResult {

        /**
         * 创建并校验成功签发结果。
         *
         * <p>Creates and validates a successful issuance result.</p>
         *
         * @throws NullPointerException 当验证码或过期时间为 {@code null} 时 / if the code or expiration
         *     instant is {@code null}
         */
        public Issued {
            Objects.requireNonNull(code, "code must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        /**
         * 返回隐藏明文验证码的安全字符串表示。
         *
         * <p>Returns a safe string representation with the plaintext code redacted.</p>
         *
         * @return 脱敏后的字符串表示 / a redacted string representation
         */
        @Override
        public String toString() {
            return "Issued[code=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }

    /**
     * 表示验证码因重发间隔尚未结束而未签发。
     *
     * <p>Indicates that issuance was rejected because the resend interval has not
     * elapsed.</p>
     *
     * @param retryAfter 距离允许再次签发的剩余时间 / the remaining duration before issuance may be retried
     */
    record Throttled(Duration retryAfter) implements IssueResult {

        /**
         * 创建并校验限流签发结果。
         *
         * <p>Creates and validates a throttled issuance result.</p>
         *
         * @throws NullPointerException 当剩余时间为 {@code null} 时 / if the retry duration is {@code null}
         * @throws IllegalArgumentException 当剩余时间为负数时 / if the retry duration is negative
         */
        public Throttled {
            Objects.requireNonNull(retryAfter, "retryAfter must not be null");
            if (retryAfter.isNegative()) {
                throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
            }
        }
    }
}
