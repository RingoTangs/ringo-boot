package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitQuota;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitStore;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** 使用 Redis ZSET 和 Lua 脚本存储限流窗口状态，并原子执行多条验证码签发限流规则。 */
public final class RedisIssueRateLimitStore implements IssueRateLimitStore {

    private static final String STORAGE_VERSION = "v2";
    private static final String KEY_DIGEST_DOMAIN = "issue-limit-bucket:v2";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> ACQUIRE_SCRIPT = RedisScript.of("""
            local now = tonumber(ARGV[1])
            local token = ARGV[2]
            local throttled = false
            local maxRetryAfter = 0

            for i, key in ipairs(KEYS) do
                local offset = 2 + (i - 1) * 2
                local window = tonumber(ARGV[offset + 1])
                local maxIssues = tonumber(ARGV[offset + 2])
                redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)
                if redis.call('ZCARD', key) >= maxIssues then
                    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                    local retryAfter = tonumber(oldest[2]) + window - now
                    if retryAfter > maxRetryAfter then
                        maxRetryAfter = retryAfter
                    end
                    throttled = true
                end
            end

            if throttled then
                return {1, maxRetryAfter}
            end

            for i, key in ipairs(KEYS) do
                local offset = 2 + (i - 1) * 2
                local window = tonumber(ARGV[offset + 1])
                redis.call('ZADD', key, now, token .. ':' .. i)
                redis.call('PEXPIRE', key, window)
            end
            return {0, 0}
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    private final byte[] secret;
    private final String applicationName;

    /** 创建 Redis 验证码签发限流状态存储。 */
    public RedisIssueRateLimitStore(StringRedisTemplate redisTemplate, byte[] secret, String applicationName) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null");
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("secret must contain at least 32 bytes");
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
    public IssueLimitResult acquire(List<IssueLimitQuota> quotas, Instant requestedAt) throws IssueRateLimitException {
        Objects.requireNonNull(quotas, "quotas must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (quotas.isEmpty()) {
            return ALLOWED;
        }

        List<String> keys = new ArrayList<>(quotas.size());
        List<String> arguments = new ArrayList<>(2 + quotas.size() * 2);
        arguments.add(Long.toString(requestedAt.toEpochMilli()));
        arguments.add(UUID.randomUUID().toString());
        for (IssueLimitQuota quota : quotas) {
            Objects.requireNonNull(quota, "quota must not be null");
            long windowMillis = quota.window().toMillis();
            if (windowMillis <= 0) {
                throw new IllegalArgumentException(
                        "Redis issue rate limit window must be at least one millisecond: " + quota.window());
            }
            keys.add(redisKey(quota.ruleId(), quota.bucket()));
            arguments.add(Long.toString(windowMillis));
            arguments.add(Integer.toString(quota.maxIssues()));
        }

        List<?> result;
        try {
            result = redisTemplate.execute(ACQUIRE_SCRIPT, keys, arguments.toArray());
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

    private String redisKey(String ruleId, IssueLimitBucket bucket) {
        String hashTag = applicationName + ":verification:issue-limit";
        return applicationName + ":verification:issue-limit:{" + hashTag + "}:" + STORAGE_VERSION + ':' + ruleId + ':'
                + bucketDigest(ruleId, bucket);
    }

    private String bucketDigest(String ruleId, IssueLimitBucket bucket) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, KEY_DIGEST_DOMAIN);
            update(mac, applicationName);
            update(mac, ruleId);
            for (String segment : bucket.segments()) {
                update(mac, segment);
            }
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
