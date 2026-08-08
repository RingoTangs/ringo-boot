package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 定义验证码状态的原子存储契约。实现不得持久化明文验证码。
 * 同一验证码键的签发、比对、尝试次数扣减和消费必须具备原子性。
 *
 * <p>Defines the atomic storage contract for verification state. Implementations must
 * not persist plaintext codes. Issuance, comparison, attempt decrement, and consumption
 * for the same verification key must be atomic.</p>
 *
 * @implNote 分布式实现必须保证跨进程的原子操作语义 / Distributed implementations must preserve atomic
 *     semantics across processes.
 */
public interface VerificationStore {

    /**
     * 尝试保存新验证码状态，并原子地执行重发间隔检查。
     * 实现可以在本次调用期间读取明文验证码，但只能保存不可逆的安全表示。
     *
     * <p>Attempts to store new verification state while atomically enforcing the resend
     * interval. An implementation may read the plaintext code during this call, but it
     * must persist only a secure, non-reversible representation.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 新签发的明文验证码 / the newly issued plaintext code
     * @param policy 验证码策略 / the verification policy
     * @param issuedAt 签发时间 / the issuance instant
     * @return 存储或限流结果 / the stored or throttled result
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt);

    /**
     * 原子地校验验证码；成功或次数耗尽时消费记录，不匹配时扣减剩余尝试次数。
     *
     * <p>Atomically verifies a code, consuming the record after success or exhausted
     * attempts and decrementing the remaining attempts after a mismatch.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待校验的明文验证码 / the plaintext code to verify
     * @param verifiedAt 校验时间 / the verification instant
     * @return 校验结果 / the verification result
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt);

    /**
     * 当验证码键与明文验证码同时匹配时原子地删除记录。
     *
     * <p>Atomically removes a record only when both its key and plaintext code match.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待失效的明文验证码 / the plaintext code to invalidate
     * @return 是否删除了匹配记录 / whether a matching record was removed
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时 / if the key or code is {@code null}
     */
    boolean invalidate(VerificationKey key, String code);

    /**
     * 表示存储层对验证码签发的处理结果。
     *
     * <p>Represents the storage decision for a verification issuance.</p>
     */
    sealed interface StoreResult permits StoreResult.Stored, StoreResult.Throttled {

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
}
