package io.github.ringotangs.ringoboot.verification.store;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 提供线程安全的内存验证码存储。验证码使用进程内随机密钥生成 HMAC-SHA256
 * 摘要，并通过常量时间比较完成校验。
 *
 *
 * <p><strong>API 注意事项：</strong> 仅适用于测试、本地开发和单实例应用。状态不会持久化或跨实例共享，进程重启后全部失效。
 *
 */
public final class InMemoryVerificationStore implements VerificationStore {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;

    private final ConcurrentMap<VerificationKey, Entry> entries = new ConcurrentHashMap<>();
    private final byte[] secret;

    /**
     * 使用新的 {@link SecureRandom} 生成进程内摘要密钥并创建内存存储。
     *
     */
    public InMemoryVerificationStore() {
        this(new SecureRandom());
    }

    /**
     * 使用指定随机源生成进程内摘要密钥并创建内存存储。
     *
     *
     * @param random 密码学安全随机源
     * @throws NullPointerException 当随机源为 {@code null} 时
     */
    public InMemoryVerificationStore(SecureRandom random) {
        Objects.requireNonNull(random, "random must not be null");
        this.secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
    }

    /**
     * 原子地保存验证码摘要，并覆盖同一验证码键的旧状态。
     *
     *
     * @param key 验证码键
     * @param code 新签发的明文验证码，仅在调用期间使用
     * @param policy 验证码策略
     * @param issuedAt 签发时间
     * @return 成功存储后的结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    @Override
    public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Instant expiresAt = issuedAt.plus(policy.ttl());
        entries.put(key, new Entry(digest(key, code), expiresAt, policy.maxAttempts()));
        return new StoreResult(expiresAt);
    }

    /**
     * 原子地比对验证码摘要，处理过期、尝试次数扣减及成功后消费。
     *
     *
     * @param key 验证码键
     * @param code 待校验的明文验证码，仅在调用期间使用
     * @param verifiedAt 校验时间
     * @return 校验结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    @Override
    public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        AtomicReference<VerificationResult> result = new AtomicReference<>(VerificationResult.NOT_FOUND);
        byte[] candidateDigest = digest(key, code);
        entries.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return null;
            }
            if (!verifiedAt.isBefore(existing.expiresAt())) {
                result.set(VerificationResult.EXPIRED);
                return null;
            }
            if (MessageDigest.isEqual(existing.digest(), candidateDigest)) {
                result.set(VerificationResult.SUCCESS);
                return null;
            }
            int remainingAttempts = existing.remainingAttempts() - 1;
            if (remainingAttempts <= 0) {
                result.set(VerificationResult.ATTEMPTS_EXHAUSTED);
                return null;
            }
            result.set(VerificationResult.MISMATCH);
            return existing.withRemainingAttempts(remainingAttempts);
        });
        return result.get();
    }

    /**
     * 通过摘要匹配原子地删除指定验证码，避免删除同一键下后来签发的新验证码。
     *
     *
     * @param key 验证码键
     * @param code 待失效的明文验证码
     * @return 是否删除了匹配记录
     */
    @Override
    public boolean invalidate(VerificationKey key, String code) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        byte[] candidateDigest = digest(key, code);
        AtomicBoolean invalidated = new AtomicBoolean();
        entries.computeIfPresent(key, (ignored, existing) -> {
            if (MessageDigest.isEqual(existing.digest(), candidateDigest)) {
                invalidated.set(true);
                return null;
            }
            return existing;
        });
        return invalidated.get();
    }

    private byte[] digest(VerificationKey key, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, key.namespace());
            update(mac, key.purpose());
            update(mac, key.subject());
            update(mac, code);
            return mac.doFinal();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    private record Entry(byte[] digest, Instant expiresAt, int remainingAttempts) {

        Entry withRemainingAttempts(int attempts) {
            return new Entry(digest, expiresAt, attempts);
        }
    }
}
