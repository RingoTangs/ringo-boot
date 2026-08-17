package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示验证码签发及交付流程的安全结果，不包含明文验证码。
 *
 * <p>Represents the safe outcome of issuing and delivering a verification code without
 * exposing the plaintext code.</p>
 */
public sealed interface DeliveryResult
        permits DeliveryResult.Accepted, DeliveryResult.Uncertain, DeliveryResult.Throttled {

    /**
     * 表示验证码已成功签发，并且发送供应商明确接受了请求。
     *
     * <p>Indicates that the code was issued and the delivery provider explicitly accepted the
     * request.</p>
     *
     * @param expiresAt 验证码过期时间 / the code expiration instant
     */
    record Accepted(Instant expiresAt) implements DeliveryResult {

        /**
         * 创建并校验成功交付结果。
         *
         * <p>Creates and validates a successful delivery result.</p>
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时 / if the expiration instant is
         *     {@code null}
         */
        public Accepted {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码已成功签发，但无法确认发送供应商是否接受请求。
     *
     * <p>Indicates that the code was issued but provider acceptance could not be determined.</p>
     *
     * @param expiresAt 验证码过期时间 / the instant at which the code expires
     */
    record Uncertain(Instant expiresAt) implements DeliveryResult {

        /** 创建并校验不确定交付结果。 / Creates and validates an uncertain delivery result. */
        public Uncertain {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码因重发间隔尚未结束而未签发或交付。
     *
     * <p>Indicates that the code was neither issued nor delivered because the resend
     * interval has not elapsed.</p>
     *
     * @param retryAfter 距离允许再次签发的剩余时间 / the remaining duration before issuance may be retried
     */
    record Throttled(Duration retryAfter) implements DeliveryResult {

        /**
         * 创建并校验限流交付结果。
         *
         * <p>Creates and validates a throttled delivery result.</p>
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
