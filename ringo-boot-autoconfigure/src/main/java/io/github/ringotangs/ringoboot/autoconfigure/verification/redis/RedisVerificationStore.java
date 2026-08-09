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
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 使用 Redis Hash 和单键 Lua 脚本原子地存储验证码状态。
 *
 * <p>Redis key 的格式如下：</p>
 *
 * <pre>{@code
 * ringo:verification:v1:{applicationName}:{namespace}:{purpose}:{keyDigest}
 * }</pre>
 *
 * <p>{@code namespace} 和 {@code purpose} 以明文保留，便于按业务分类；邮箱、手机号等 {@code subject}
 * 不会出现在 key 中。{@code keyDigest} 的生成方式为：</p>
 *
 * <pre>{@code
 * Base64UrlWithoutPadding(
 *     HMAC-SHA256(secret, "key:v1", applicationName, namespace, purpose, subject)
 * )
 * }</pre>
 *
 * <p>每个 key 对应一个 Redis Hash，包含以下字段：</p>
 *
 * <ul>
 *   <li>{@code codeDigest}：绑定业务维度、subject 和验证码的 HMAC-SHA256 摘要；</li>
 *   <li>{@code expiresAt}：验证码业务过期时间，使用 epoch milliseconds；</li>
 *   <li>{@code resendAt}：允许再次签发验证码的时间，使用 epoch milliseconds；</li>
 *   <li>{@code remainingAttempts}：剩余校验次数。</li>
 * </ul>
 *
 * <p>例如，Redis 中的数据形态可能是：</p>
 *
 * <pre>{@code
 * key: ringo:verification:v1:identity-service:account:email-verification:AbCdEf...
 * hash:
 *   codeDigest        XyZ...
 *   expiresAt         1786266000000
 *   resendAt          1786265760000
 *   remainingAttempts 5
 * }</pre>
 *
 * <p>Redis key 的 TTL 截止时间为 {@code expiresAt + expiredRetention}。校验成功、尝试次数耗尽、主动失效，
 * 或校验时发现业务已过期，都会提前删除记录。邮箱、手机号和验证码均不会以明文写入 Redis。</p>
 *
 * <p>Atomically stores verification state with Redis hashes and single-key Lua scripts. A Redis
 * key has the form {@code
 * ringo:verification:v1:{applicationName}:{namespace}:{purpose}:{keyDigest}}. The application name,
 * namespace, and purpose remain readable for isolation and classification, while the subject is
 * included only in the HMAC-SHA256 key digest. Each hash stores {@code codeDigest}, {@code expiresAt}, {@code
 * resendAt}, and {@code remainingAttempts}. Timestamps use epoch milliseconds, and the Redis key
 * expires at {@code expiresAt + expiredRetention}. Successful verification, exhausted attempts,
 * explicit invalidation, and detection of business expiration delete the record early. Email
 * addresses, phone numbers, and verification codes are never stored in plaintext.</p>
 *
 * @apiNote 所有共享 Redis 数据的应用实例必须使用同一 HMAC 密钥，否则相同验证键会映射到不同 Redis
 *     key，且验证码摘要无法匹配。 / Every application instance sharing Redis data must use the
 *     same HMAC secret; otherwise identical verification keys map to different Redis keys and code
 *     digests cannot match.
 */
public final class RedisVerificationStore implements VerificationStore {

    private static final String KEY_PREFIX = "ringo:verification:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /**
     * 原子签发脚本。
     *
     * <p>{@code KEYS[1]} 是完整 Redis key。参数依次为：{@code ARGV[1]} 验证码摘要、{@code ARGV[2]}
     * 业务过期时间、{@code ARGV[3]} 可重发时间、{@code ARGV[4]} 最大尝试次数、{@code ARGV[5]}
     * Redis 删除时间、{@code ARGV[6]} 本次签发时间，所有时间均为 epoch milliseconds。</p>
     *
     * <p>未到已有记录的重发时间时返回 {@code [1, retryAfterMillis]}；否则覆盖 Hash、设置绝对 TTL，
     * 并返回 {@code [0, expiresAtMillis]}。检查、写入和设置 TTL 在一个 Lua 脚本中完成，避免并发重发绕过限流。</p>
     *
     * <p>Atomic issuance script. {@code KEYS[1]} is the complete Redis key. Arguments are the code
     * digest, business expiration, resend time, maximum attempts, Redis deletion time, and current
     * issuance time. It returns {@code [1, retryAfterMillis]} when issuance is throttled, otherwise
     * replaces the hash, sets its absolute TTL, and returns {@code [0, expiresAtMillis]}.</p>
     */
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

    /**
     * 原子校验并消费脚本。
     *
     * <p>{@code KEYS[1]} 是完整 Redis key；{@code ARGV[1]} 是待校验验证码摘要；{@code ARGV[2]}
     * 是校验时间的 epoch milliseconds。返回码依次表示：{@code 0} 不存在、{@code 1} 已过期、{@code 2}
     * 成功、{@code 3} 不匹配但仍可尝试、{@code 4} 尝试次数耗尽、{@code 5} Hash 数据不完整。</p>
     *
     * <p>成功、过期和次数耗尽都会删除 key；普通不匹配会原子递减 {@code remainingAttempts}。</p>
     *
     * <p>Atomic verify-and-consume script. {@code KEYS[1]} is the complete Redis key, {@code
     * ARGV[1]} is the candidate code digest, and {@code ARGV[2]} is the verification time in epoch
     * milliseconds. Statuses {@code 0} through {@code 5} mean not found, expired, success,
     * mismatch, attempts exhausted, and incomplete state. Success, expiration, and exhaustion
     * delete the key; an ordinary mismatch atomically decrements the remaining attempts.</p>
     */
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

    /**
     * 原子失效脚本。
     *
     * <p>{@code KEYS[1]} 是完整 Redis key，{@code ARGV[1]} 是需要撤销的验证码摘要。只有 Hash 中保存的摘要
     * 仍与参数相同时才删除记录，避免旧派发任务失败后的补偿操作误删后来签发的新验证码。</p>
     *
     * <p>Atomic invalidation script. {@code KEYS[1]} is the complete Redis key and {@code ARGV[1]}
     * is the code digest to revoke. It deletes the hash only while the stored digest still matches,
     * preventing compensation for an older delivery failure from deleting a newer code.</p>
     */
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
    private final @Nullable String applicationName;

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
    @Deprecated(since = "1.0", forRemoval = false)
    public RedisVerificationStore(StringRedisTemplate redisTemplate, byte[] secret, Duration expiredRetention) {
        this(redisTemplate, secret, expiredRetention, null, true);
    }

    /**
     * 使用应用名称隔离 Redis 数据，创建验证码存储。
     *
     * <p>Creates a verification store whose Redis data is isolated by application name.</p>
     *
     * @param redisTemplate Redis 字符串模板 / the Redis string template
     * @param secret 至少 32 字节的共享 HMAC 密钥 / the shared HMAC secret of at least 32 bytes
     * @param expiredRetention 业务过期后的保留时间 / retention after business expiration
     * @param applicationName Redis key 和摘要使用的应用名称 / application name used by Redis
     *     keys and digests
     */
    public RedisVerificationStore(
            StringRedisTemplate redisTemplate, byte[] secret, Duration expiredRetention, String applicationName) {
        this(redisTemplate, secret, expiredRetention, applicationName, false);
    }

    private RedisVerificationStore(
            StringRedisTemplate redisTemplate,
            byte[] secret,
            Duration expiredRetention,
            @Nullable String applicationName,
            boolean legacyFormat) {
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
        if (!legacyFormat) {
            Objects.requireNonNull(applicationName, "applicationName must not be null");
            if (!APPLICATION_NAME_PATTERN.matcher(applicationName).matches()) {
                throw new IllegalArgumentException(
                        "applicationName must start with an alphanumeric character and contain only letters, digits, '.', '_', or '-': "
                                + applicationName);
            }
        }
        this.applicationName = applicationName;
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

    /**
     * 创建验证码状态对应的 Redis key。
     *
     * <p>Creates the Redis key for the verification state.</p>
     *
     * @param key 包含业务域、用途和验证主体的验证键 / verification key containing namespace,
     *     purpose, and subject
     * @return 包含可读业务前缀和不可逆 subject 摘要的 Redis key / Redis key containing readable
     *     business segments and an irreversible subject digest
     */
    private String redisKey(VerificationKey key) {
        String applicationSegment = applicationName == null ? "" : applicationName + ':';
        return KEY_PREFIX
                + applicationSegment
                + key.namespace()
                + ':'
                + key.purpose()
                + ':'
                + digest("key:v1", key, null);
    }

    /**
     * 创建与业务维度和验证主体绑定的验证码摘要。
     *
     * <p>相同验证码用于不同 subject、namespace 或 purpose 时会生成不同摘要。</p>
     *
     * <p>Creates a code digest bound to the business dimensions and verification subject. The same
     * code produces a different digest for another subject, namespace, or purpose.</p>
     *
     * @param key 验证键 / verification key
     * @param code 验证码明文，仅在当前进程中参与摘要计算 / plaintext code used only for digest
     *     calculation in the current process
     * @return 无填充 Base64URL 编码的 HMAC-SHA256 摘要 / unpadded Base64URL-encoded HMAC-SHA256
     *     digest
     */
    private String codeDigest(VerificationKey key, String code) {
        return digest("code:v1", key, code);
    }

    /**
     * 按确定的分段编码计算 HMAC-SHA256 摘要。
     *
     * <p>{@code domain} 用于隔离 Redis key 摘要和验证码摘要，防止相同输入在不同用途间复用。各字符串段通过
     * {@link #update(Mac, String)} 编码，顺序为 domain、applicationName、namespace、purpose、subject，
     * 以及可选 code。兼容构造器省略 applicationName 段。</p>
     *
     * <p>Computes an HMAC-SHA256 digest over deterministically framed segments. The domain separates
     * Redis-key digests from code digests. Segment order is domain, application name, namespace,
     * purpose, subject, and the optional code. The compatibility constructor omits the application
     * name segment.</p>
     *
     * @param domain 摘要用途域 / digest-purpose domain
     * @param key 验证键 / verification key
     * @param code 可选验证码；生成 Redis key 摘要时为 {@code null} / optional code, {@code null}
     *     when creating a Redis-key digest
     * @return 无填充 Base64URL 编码摘要 / unpadded Base64URL-encoded digest
     * @throws VerificationStoreException 当运行环境不支持 HmacSHA256 时 / if HmacSHA256 is not
     *     available
     */
    private String digest(String domain, VerificationKey key, @Nullable String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, domain);
            if (applicationName != null) {
                update(mac, applicationName);
            }
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

    /**
     * 将一个字符串段以“4 字节大端长度 + UTF-8 内容”写入 HMAC。
     *
     * <p>长度前缀保留分段边界，避免 {@code ["ab", "c"]} 与 {@code ["a", "bc"]} 产生相同输入字节。</p>
     *
     * <p>Feeds one string segment into the HMAC as a four-byte big-endian length followed by UTF-8
     * bytes. Length framing preserves segment boundaries, so {@code ["ab", "c"]} cannot collide
     * with {@code ["a", "bc"]} merely through concatenation.</p>
     *
     * @param mac 当前 HMAC 计算器 / active HMAC calculator
     * @param value 要写入的字符串段 / string segment to feed
     */
    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    /**
     * 使用单个 Redis key 执行 Lua 脚本并统一转换 Spring Data Redis 访问异常。
     *
     * <p>Executes a Lua script against one Redis key and consistently translates Spring Data Redis
     * access failures.</p>
     *
     * @param script 要执行的脚本 / script to execute
     * @param key 唯一的 Redis key / sole Redis key
     * @param arguments 脚本 ARGV 参数 / script ARGV values
     * @param <T> 脚本返回类型 / script result type
     * @return 脚本结果，Redis 返回空结果时可能为 {@code null} / script result, possibly {@code
     *     null} when Redis returns no value
     * @throws VerificationStoreException 当 Redis 访问失败时 / if Redis access fails
     */
    private <T extends @Nullable Object> @Nullable T execute(RedisScript<T> script, String key, String... arguments) {
        try {
            return redisTemplate.execute(script, List.of(key), (Object[]) arguments);
        } catch (DataAccessException exception) {
            throw new VerificationStoreException("Redis verification operation failed", exception);
        }
    }

    /**
     * 从 Store Lua 脚本返回数组中读取数字，并拒绝缺失或类型错误的响应。
     *
     * <p>Reads a number from the Store Lua result array and rejects missing or malformed results.</p>
     *
     * @param result Lua 返回数组 / Lua result array
     * @param index 数字所在索引 / index containing the number
     * @return 转换后的 long 值 / converted long value
     * @throws VerificationStoreException 当返回数据不符合脚本协议时 / if the result violates the
     *     script protocol
     */
    private long number(@Nullable List<?> result, int index) {
        if (result == null || result.size() <= index || !(result.get(index) instanceof Number value)) {
            throw new VerificationStoreException("Redis store script returned an invalid result");
        }
        return value.longValue();
    }
}
