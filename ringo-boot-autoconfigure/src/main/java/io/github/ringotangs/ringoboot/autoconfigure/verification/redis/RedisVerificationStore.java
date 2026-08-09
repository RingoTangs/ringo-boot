package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 使用 Redis Hash 和单键 Lua 脚本原子地存储验证码状态。
 *
 * <p>Atomically stores verification state with Redis hashes and single-key Lua
 * scripts.</p>
 *
 * @apiNote Redis 中只保存不可逆 HMAC 摘要，所有应用实例必须使用同一共享密钥。 / Redis stores only
 *     irreversible HMAC digests, and every application instance must use the same
 *     shared secret.
 */
public final class RedisVerificationStore implements VerificationStore {

    private static final String KEY_PREFIX = "ringo:verification:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> STORE_SCRIPT = RedisScript.of("""
            local existingExpiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
            if existingExpiresAt and tonumber(ARGV[6]) < existingExpiresAt then
                local existingResendAt = tonumber(redis.call('HGET', KEYS[1], 'resendAt'))
                if existingResendAt and tonumber(ARGV[6]) < existingResendAt then
                    return {1, existingResendAt - tonumber(ARGV[6])}
                end
            end
            redis.call('HSET', KEYS[1],
                'codeDigest', ARGV[1],
                'expiresAt', ARGV[2],
                'resendAt', ARGV[3],
                'remainingAttempts', ARGV[4])
            redis.call('PEXPIREAT', KEYS[1], ARGV[5])
            return {0, tonumber(ARGV[2])}
            """, List.class);

    private static final RedisScript<Long> VERIFY_SCRIPT = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
            local storedDigest = redis.call('HGET', KEYS[1], 'codeDigest')
            local remainingAttempts = tonumber(redis.call('HGET', KEYS[1], 'remainingAttempts'))
            if not expiresAt or not storedDigest or not remainingAttempts then
                return 5
            end
            if tonumber(ARGV[2]) >= expiresAt then
                redis.call('DEL', KEYS[1])
                return 1
            end
            if storedDigest == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 2
            end
            if remainingAttempts <= 1 then
                redis.call('DEL', KEYS[1])
                return 4
            end
            redis.call('HINCRBY', KEYS[1], 'remainingAttempts', -1)
            return 3
            """, Long.class);

    private static final RedisScript<Long> INVALIDATE_SCRIPT = RedisScript.of("""
            local storedDigest = redis.call('HGET', KEYS[1], 'codeDigest')
            if storedDigest and storedDigest == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final byte[] secret;
    private final Duration expiredRetention;

    /**
     * 使用 Redis 模板、共享密钥和过期保留时间创建存储。
     *
     * <p>Creates a store with a Redis template, shared secret, and expiration
     * retention.</p>
     *
     * @param redisTemplate Redis 字符串模板 / the Redis string template
     * @param secret 至少 32 字节的共享 HMAC 密钥 / the shared HMAC secret of at least 32 bytes
     * @param expiredRetention 业务过期后的保留时间 / retention after business expiration
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     * @throws IllegalArgumentException 当密钥过短或保留时间不是正数时 / if the secret is too short or
     *     retention is not positive
     */
    public RedisVerificationStore(StringRedisTemplate redisTemplate, byte[] secret, Duration expiredRetention) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        this.expiredRetention = Objects.requireNonNull(expiredRetention, "expiredRetention must not be null");
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("secret must contain at least 32 bytes");
        }
        if (expiredRetention.isZero() || expiredRetention.isNegative()) {
            throw new IllegalArgumentException("expiredRetention must be positive: " + expiredRetention);
        }
        this.secret = secret.clone();
    }

    /** {@inheritDoc} */
    @Override
    public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt)
            throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Instant expiresAt = issuedAt.plus(policy.ttl());
        Instant resendAt = issuedAt.plus(policy.resendInterval());
        Instant deleteAt = expiresAt.plus(expiredRetention);
        List<?> result = execute(
                STORE_SCRIPT,
                redisKey(key),
                codeDigest(key, code),
                Long.toString(expiresAt.toEpochMilli()),
                Long.toString(resendAt.toEpochMilli()),
                Integer.toString(policy.maxAttempts()),
                Long.toString(deleteAt.toEpochMilli()),
                Long.toString(issuedAt.toEpochMilli()));
        long status = number(result, 0);
        long value = number(result, 1);
        return switch ((int) status) {
            case 0 -> new StoreResult.Stored(Instant.ofEpochMilli(value));
            case 1 -> new StoreResult.Throttled(Duration.ofMillis(value));
            default -> throw new VerificationStoreException("Redis store script returned an unknown status");
        };
    }

    /** {@inheritDoc} */
    @Override
    public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt)
            throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        Long result =
                execute(VERIFY_SCRIPT, redisKey(key), codeDigest(key, code), Long.toString(verifiedAt.toEpochMilli()));
        if (result == null) {
            throw new VerificationStoreException("Redis verify script returned no result");
        }
        return switch (result.intValue()) {
            case 0 -> VerificationResult.NOT_FOUND;
            case 1 -> VerificationResult.EXPIRED;
            case 2 -> VerificationResult.SUCCESS;
            case 3 -> VerificationResult.MISMATCH;
            case 4 -> VerificationResult.ATTEMPTS_EXHAUSTED;
            case 5 -> throw new VerificationStoreException("Redis verification state is incomplete");
            default -> throw new VerificationStoreException("Redis verify script returned an unknown status");
        };
    }

    /** {@inheritDoc} */
    @Override
    public boolean invalidate(VerificationKey key, String code) throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Long result = execute(INVALIDATE_SCRIPT, redisKey(key), codeDigest(key, code));
        if (result == null) {
            throw new VerificationStoreException("Redis invalidate script returned no result");
        }
        return result == 1;
    }

    private String redisKey(VerificationKey key) {
        return KEY_PREFIX + key.namespace() + ':' + key.purpose() + ':' + digest("key:v1", key, null);
    }

    private String codeDigest(VerificationKey key, String code) {
        return digest("code:v1", key, code);
    }

    private String digest(String domain, VerificationKey key, @Nullable String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, domain);
            update(mac, key.namespace());
            update(mac, key.purpose());
            update(mac, key.subject());
            if (code != null) {
                update(mac, code);
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
        } catch (GeneralSecurityException exception) {
            throw new VerificationStoreException("HmacSHA256 is not available", exception);
        }
    }

    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    private <T extends @Nullable Object> @Nullable T execute(RedisScript<T> script, String key, String... arguments) {
        try {
            return redisTemplate.execute(script, List.of(key), (Object[]) arguments);
        } catch (DataAccessException exception) {
            throw new VerificationStoreException("Redis verification operation failed", exception);
        }
    }

    private long number(@Nullable List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number value)) {
            throw new VerificationStoreException("Redis store script returned an invalid result");
        }
        return value.longValue();
    }
}
