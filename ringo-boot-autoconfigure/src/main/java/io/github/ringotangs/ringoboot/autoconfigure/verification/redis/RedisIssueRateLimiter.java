package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** 使用 Redis TTL 和单键 Lua 脚本实现跨实例的验证码签发频率限制。 */
public final class RedisIssueRateLimiter implements IssueRateLimiter {

    private static final String STORAGE_VERSION = "v1";
    private static final String KEY_DIGEST_DOMAIN = "issue-limit-key:v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> ACQUIRE_SCRIPT = RedisScript.of("""
            local acquired = redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX')
            if acquired then
                return {0, 0}
            end
            local retryAfter = redis.call('PTTL', KEYS[1])
            if retryAfter < 0 then
                retryAfter = 0
            end
            return {1, retryAfter}
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    private final byte[] secret;
    private final Duration interval;
    private final String applicationName;

    /**
     * 创建 Redis 验证码签发限流器。
     *
     * @param redisTemplate Redis 字符串操作模板
     * @param secret 至少 32 字节的共享 HMAC 密钥
     * @param interval 同一验证码键的最小签发间隔，零表示不限制
     * @param applicationName Redis key 使用的应用名称
     */
    public RedisIssueRateLimiter(
            StringRedisTemplate redisTemplate, byte[] secret, Duration interval, String applicationName) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        this.interval = Objects.requireNonNull(interval, "interval must not be null");
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null");
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("secret must contain at least 32 bytes");
        }
        if (interval.isNegative()) {
            throw new IllegalArgumentException("interval must not be negative: " + interval);
        }
        if (!interval.isZero() && interval.toMillis() <= 0) {
            throw new IllegalArgumentException("interval must be zero or at least one millisecond: " + interval);
        }
        if (!APPLICATION_NAME_PATTERN.matcher(applicationName).matches()) {
            throw new IllegalArgumentException(
                    "applicationName must start with an alphanumeric character and contain only letters, digits, '.', '_', or '-': "
                            + applicationName);
        }
        this.secret = secret.clone();
    }

    /** {@inheritDoc} */
    @Override
    public IssueLimitResult acquire(VerificationKey key, Instant requestedAt) throws IssueRateLimitException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (interval.isZero()) {
            return ALLOWED;
        }
        List<?> result;
        try {
            result = redisTemplate.execute(
                    ACQUIRE_SCRIPT,
                    List.of(redisKey(key)),
                    Long.toString(requestedAt.toEpochMilli()),
                    Long.toString(interval.toMillis()));
        } catch (DataAccessException exception) {
            throw new IssueRateLimitException("Redis issue rate limit operation failed", exception);
        }
        long status = number(result, 0);
        long retryAfterMillis = number(result, 1);
        return switch ((int) status) {
            case 0 -> ALLOWED;
            case 1 -> new IssueLimitResult.Throttled(Duration.ofMillis(retryAfterMillis));
            default -> throw new IssueRateLimitException("Redis issue rate limit script returned an unknown status");
        };
    }

    private String redisKey(VerificationKey key) {
        return applicationName + ":verification:issue-limit:" + STORAGE_VERSION + ':' + key.namespace() + ':'
                + key.purpose() + ':' + keyDigest(key);
    }

    private String keyDigest(VerificationKey key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, KEY_DIGEST_DOMAIN);
            update(mac, applicationName);
            update(mac, key.namespace());
            update(mac, key.purpose());
            update(mac, key.subject());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
        } catch (GeneralSecurityException exception) {
            throw new IssueRateLimitException("HmacSHA256 is not available", exception);
        }
    }

    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    private long number(List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number value)) {
            throw new IssueRateLimitException("Redis issue rate limit script returned an invalid result");
        }
        return value.longValue();
    }
}
