package io.github.ringotangs.ringoboot.verification.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示存储层对验证码签发的处理结果。
 *
 */
public sealed interface StoreResult permits StoreResult.Stored, StoreResult.Throttled {

    /**
     * 表示验证码状态已成功保存。
     *
     *
     * @param expiresAt 验证码过期时间
     */
    record Stored(Instant expiresAt) implements StoreResult {

        /**
         * 创建并校验成功存储结果。
         *
         *
         * @throws NullPointerException 当过期时间为 {@code null} 时
         */
        public Stored {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码状态因重发间隔尚未结束而未保存。
     *
     *
     * @param retryAfter 距离允许再次签发的剩余时间
     */
    record Throttled(Duration retryAfter) implements StoreResult {

        /**
         * 创建并校验限流存储结果。
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
