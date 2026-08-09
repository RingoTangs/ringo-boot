package io.github.ringotangs.ringoboot.verification.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示存储层对验证码签发的处理结果。
 *
 * <p>Represents the storage decision for a verification issuance.</p>
 */
public sealed interface StoreResult permits StoreResult.Stored, StoreResult.Throttled {

    /**
     * 表示验证码状态已成功保存。
     *
     * <p>Indicates that the verification state was stored successfully.</p>
     *
     * @param expiresAt 验证码过期时间 / the instant at which the code expires
     */
    record Stored(Instant expiresAt) implements StoreResult {

        /**
         * 创建并校验成功存储结果。
         *
         * <p>Creates and validates a successful storage result.</p>
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时 / if the expiration instant is
         *     {@code null}
         */
        public Stored {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码状态因重发间隔尚未结束而未保存。
     *
     * <p>Indicates that the verification state was not stored because the resend
     * interval has not elapsed.</p>
     *
     * @param retryAfter 距离允许再次签发的剩余时间 / the remaining duration before issuance may be retried
     */
    record Throttled(Duration retryAfter) implements StoreResult {

        /**
         * 创建并校验限流存储结果。
         *
         * <p>Creates and validates a throttled storage result.</p>
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
