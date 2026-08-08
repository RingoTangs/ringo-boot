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
public sealed interface DeliveryResult permits DeliveryResult.Delivered, DeliveryResult.Throttled {

    /**
     * 表示验证码已成功签发并交付。
     *
     * <p>Indicates that the verification code was issued and delivered successfully.</p>
     *
     * @param expiresAt 验证码过期时间 / the code expiration instant
     */
    record Delivered(Instant expiresAt) implements DeliveryResult {

        /**
         * 创建并校验成功交付结果。
         *
         * <p>Creates and validates a successful delivery result.</p>
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时 / if the expiration instant is
         *     {@code null}
         */
        public Delivered {
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
