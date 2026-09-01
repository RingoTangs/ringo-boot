package io.github.ringotangs.ringoboot.verification.autoconfigure.redis;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitQuota;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStoreException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
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
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 使用 Redis 有序集合和 Lua 脚本保存验证码签发限流状态。
 *
 * <p>每个“规则标识 + 额度桶”对应一个 Redis ZSET。ZSET 的 score 是签发时间的毫秒时间戳，member 是本次请求生成的随机标识。
 * 因此实现可以删除滚动窗口外的记录，并通过集合大小判断当前窗口是否还有额度。
 *
 * <p>一次验证码签发可能同时受到接收方、IP 和应用全局等多条规则约束。本实现把这些规则对应的 key 一次性交给同一个 Lua
 * 脚本处理：脚本先清理并检查全部窗口，任一规则受限时不写入任何记录；只有全部规则允许时才同时消费所有额度，从而保证全有或全无。
 *
 * <p>Redis key 的格式如下，实际内容为一行：
 *
 * <pre>{@code
 * {identity-service:verification:issue-limit}:v1:login-subject-minute:{bucketDigest}
 * }</pre>
 *
 * <p>花括号中的应用级哈希标签使同一次请求涉及的全部 key 位于 Redis Cluster 的同一个 slot，以满足多 key Lua
 * 脚本的执行要求。相应地，同一应用的限流 key 也会集中到该 slot，部署时应结合实际签发流量评估容量。
 *
 * <p>额度桶可能包含手机号、邮箱或 IP。原始分段不会写入 Redis，而是与摘要域、应用名称和规则标识一起计算
 * HMAC-SHA256，再编码为无填充 Base64URL 字符串。共享限流状态的所有应用实例必须使用相同的应用名称和 HMAC 密钥。
 *
 * <p>当前实现使用调用方传入的 {@code requestedAt} 作为滚动窗口时间。多实例部署时应保持各应用服务器时钟同步。
 */
public final class RedisIssueLimitStore implements IssueLimitStore {

    /**
     * Redis key 与摘要协议的版本，用于隔离未来不兼容的存储格式。
     */
    private static final String STORAGE_VERSION = "v1";

    /**
     * 写入 HMAC 的固定摘要域，避免相同输入被其他摘要用途复用。
     */
    private static final String KEY_DIGEST_DOMAIN = "issue-limit-bucket:v1";

    /**
     * 额度桶摘要使用的 HMAC 算法。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * HMAC 密钥允许的最小字节数。
     */
    private static final int MINIMUM_SECRET_BYTES = 32;

    /**
     * Redis key 中应用名称允许使用的字符格式。
     */
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /**
     * 不包含状态的允许结果，可在额度获取成功时安全复用。
     */
    private static final IssueLimitResult.Allowed ALLOWED = new IssueLimitResult.Allowed();

    /**
     * 原子检查并消费全部签发额度的 Lua 脚本。
     *
     * <p>{@code KEYS} 按配额顺序保存全部限流 key。{@code ARGV[1]} 是请求时间的毫秒时间戳，{@code ARGV[2]}
     * 是本次请求的随机标识，后续参数按“窗口毫秒数、最大签发次数”成对排列。
     *
     * <p>脚本首先从每个 ZSET 中删除 score 小于等于“当前时间减去窗口”的记录，然后检查集合大小。存在受限规则时返回
     * {@code {1, quotaIndex, retryAfterMillis, ...}}，且不会向任何 ZSET 写入数据；全部规则允许时，脚本向每个 ZSET
     * 写入当前请求并把 TTL 设置为对应窗口，最后返回 {@code {0}}。
     */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> ACQUIRE_SCRIPT = RedisScript.of("""
            local now = tonumber(ARGV[1])
            local token = ARGV[2]
            local violations = {}

            for i, key in ipairs(KEYS) do
                local offset = 2 + (i - 1) * 2
                local window = tonumber(ARGV[offset + 1])
                local maxIssues = tonumber(ARGV[offset + 2])
                redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)
                if redis.call('ZCARD', key) >= maxIssues then
                    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                    local retryAfter = tonumber(oldest[2]) + window - now
                    table.insert(violations, i)
                    table.insert(violations, retryAfter)
                end
            end

            if #violations > 0 then
                local result = {1}
                for _, value in ipairs(violations) do
                    table.insert(result, value)
                end
                return result
            end

            for i, key in ipairs(KEYS) do
                local offset = 2 + (i - 1) * 2
                local window = tonumber(ARGV[offset + 1])
                redis.call('ZADD', key, now, token .. ':' .. i)
                redis.call('PEXPIRE', key, window)
            end
            return {0}
            """, List.class);

    /**
     * 执行 Lua 脚本的 Redis 字符串操作模板。
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 用于隐藏额度桶敏感分段的 HMAC 密钥副本。
     */
    private final byte[] secret;

    /**
     * 用于隔离不同应用 Redis key 和摘要空间的稳定应用名称。
     */
    private final String applicationName;

    /**
     * 创建 Redis 验证码签发限流状态存储。
     *
     * @param redisTemplate   Redis 字符串操作模板
     * @param secret          至少 32 字节的共享 HMAC 密钥
     * @param applicationName Redis key 使用的应用名称
     * @throws NullPointerException     当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当密钥少于 32 字节，或者应用名称格式非法时
     */
    public RedisIssueLimitStore(StringRedisTemplate redisTemplate, byte[] secret, String applicationName) {
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

    /**
     * 原子检查并消费本次签发涉及的全部额度。
     *
     * <p>方法先为每条配额生成 Redis key，并把窗口和最大次数转换为 Lua 参数。窗口必须能够表示为至少一毫秒。Lua
     * 脚本返回状态 {@code 0} 时表示所有额度已经成功消费；返回状态 {@code 1} 时表示本次没有消费任何额度，并返回每条受限规则的
     * quota 索引及剩余等待时间。
     *
     * <p>传入时间直接作为所有 ZSET 的 score 和窗口计算基准。该值不会被 Redis 服务器时间替换。
     *
     * @param quotas      本次请求需要同时满足的非空签发配额集合
     * @param requestedAt 请求签发的时间
     * @return 全部配额允许时返回 {@link IssueLimitResult.Allowed}，否则返回包含全部受限规则明细的
     *     {@link IssueLimitResult.Throttled}
     * @throws NullPointerException     当配额集合、任一配额或请求时间为 {@code null} 时
     * @throws IllegalArgumentException 当配额集合为空，或者任一窗口不足一毫秒时
     * @throws IssueLimitStoreException 当 Redis 操作失败，或者脚本返回未知或非法结果时
     */
    @Override
    public IssueLimitResult acquire(List<IssueLimitQuota> quotas, Instant requestedAt) throws IssueLimitStoreException {
        Objects.requireNonNull(quotas, "quotas must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (quotas.isEmpty()) {
            throw new IllegalArgumentException("quotas must not be empty");
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

        @Nullable List<?> result;
        try {
            result = redisTemplate.execute(ACQUIRE_SCRIPT, keys, arguments.toArray());
        } catch (DataAccessException exception) {
            throw new IssueLimitStoreException("Redis issue rate limit operation failed", exception);
        }
        long status = number(result, 0);
        if (status == 0) {
            if (result.size() != 1) {
                throw new IssueLimitStoreException("Redis issue rate limit script returned an invalid result");
            }
            return ALLOWED;
        }
        if (status == 1) {
            return throttled(result, quotas);
        }
        throw new IssueLimitStoreException("Redis issue rate limit script returned an unknown status");
    }

    private IssueLimitResult.Throttled throttled(List<?> result, List<IssueLimitQuota> quotas) {
        if (result.size() < 3 || result.size() % 2 == 0) {
            throw new IssueLimitStoreException("Redis issue rate limit script returned an invalid result");
        }
        List<IssueLimitViolation> violations = new ArrayList<>((result.size() - 1) / 2);
        boolean[] seen = new boolean[quotas.size()];
        for (int position = 1; position < result.size(); position += 2) {
            long quotaIndex = number(result, position);
            long retryAfterMillis = number(result, position + 1);
            if (quotaIndex < 1 || quotaIndex > quotas.size() || retryAfterMillis < 0) {
                throw new IssueLimitStoreException("Redis issue rate limit script returned an invalid result");
            }
            int index = Math.toIntExact(quotaIndex - 1);
            if (seen[index]) {
                throw new IssueLimitStoreException("Redis issue rate limit script returned an invalid result");
            }
            seen[index] = true;
            violations.add(new IssueLimitViolation(quotas.get(index).ruleId(), Duration.ofMillis(retryAfterMillis)));
        }
        return new IssueLimitResult.Throttled(violations);
    }

    /**
     * 创建一条规则下某个额度桶对应的 Redis key。
     *
     * <p>完整业务范围直接作为 Cluster 哈希标签，不再重复业务前缀。规则标识保持可读，额度桶只以 HMAC 摘要形式出现。
     *
     * @param ruleId 产生额度的稳定规则标识
     * @param bucket 包含业务累计身份分段的额度桶
     * @return 带应用隔离、Cluster 哈希标签、存储版本和额度桶摘要的 Redis key
     */
    private String redisKey(String ruleId, IssueLimitBucket bucket) {
        String hashTag = applicationName + ":verification:issue-limit";
        return '{' + hashTag + "}:" + STORAGE_VERSION + ':' + ruleId + ':' + bucketDigest(ruleId, bucket);
    }

    /**
     * 计算与应用、规则和额度桶绑定的安全摘要。
     *
     * <p>摘要输入依次为固定域、应用名称、规则标识和全部额度桶分段。每个字符串都使用长度前缀编码，避免不同分段组合产生相同的字节序列。
     *
     * @param ruleId 产生额度的稳定规则标识
     * @param bucket 包含手机号、邮箱或 IP 等潜在敏感分段的额度桶
     * @return 无填充 Base64URL 编码的 HMAC-SHA256 摘要
     * @throws IssueLimitStoreException 当运行环境不支持 HmacSHA256 时
     */
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
            throw new IssueLimitStoreException("HmacSHA256 is not available", exception);
        }
    }

    /**
     * 使用“4 字节大端长度 + UTF-8 内容”的格式向 HMAC 写入一个字符串分段。
     *
     * <p>长度前缀可以区分 {@code ["ab", "c"]} 与 {@code ["a", "bc"]}，避免直接拼接字符串造成边界冲突。
     *
     * @param mac   正在计算的 HMAC
     * @param value 待写入的字符串分段
     */
    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    /**
     * 从 Lua 返回列表中读取一个整数结果。
     *
     * @param result Redis 脚本返回的列表
     * @param index  待读取的元素位置
     * @return 指定位置的整数值
     * @throws IssueLimitStoreException 当返回值为空、长度不足或指定元素不是数字时
     */
    private long number(@Nullable List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number value)) {
            throw new IssueLimitStoreException("Redis issue rate limit script returned an invalid result");
        }
        return value.longValue();
    }
}
