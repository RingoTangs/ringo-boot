package io.github.ringotangs.ringoboot.verification.redis;

import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreKey;
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
 * {applicationName}:verification:v1:{namespace}:{purpose}:{keyDigest}
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
 *   <li>{@code remainingAttempts}：剩余校验次数。</li>
 * </ul>
 *
 * <p>例如，Redis 中的数据形态可能是：</p>
 *
 * <pre>{@code
 * key: identity-service:verification:v1:account:email-verification:AbCdEf...
 * hash:
 *   codeDigest        XyZ...
 *   expiresAt         1786266000000
 *   remainingAttempts 5
 * }</pre>
 *
 * <p>Redis key 的 TTL 截止时间为 {@code expiresAt + expiredRetention}。校验成功、尝试次数耗尽、主动失效，
 * 或校验时发现业务已过期，都会提前删除记录。邮箱、手机号和验证码均不会以明文写入 Redis。</p>
 *
 * <p>{@code v1} 是框架维护的存储协议版本，不是运行时配置。未来发生不兼容的 key、Hash 字段或摘要协议
 * 变更时，可使用新版本隔离新旧数据，避免新代码错误解释旧数据。</p>
 *
 * <p>所有共享 Redis 数据的应用实例必须使用同一 HMAC 密钥，否则相同验证码键会映射到不同 Redis key，验证码摘要也无法匹配。</p>
 */
public final class RedisVerificationStore implements VerificationStore {

    private static final String STORAGE_VERSION = "v1";
    private static final String KEY_DIGEST_DOMAIN = "key:v1";
    private static final String CODE_DIGEST_DOMAIN = "code:v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Pattern APPLICATION_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /**
     * 原子存储脚本。
     *
     * <p>{@code KEYS[1]} 是完整 Redis key。参数依次为：{@code ARGV[1]} 验证码摘要、{@code ARGV[2]}
     * 业务过期时间、{@code ARGV[3]} 最大尝试次数、{@code ARGV[4]} Redis 删除时间，
     * 所有时间均为 epoch milliseconds。</p>
     *
     * <p>脚本会完整覆盖同一验证码键的旧 Hash、设置绝对 TTL，并返回业务过期时间。</p>
     */
    private static final RedisScript<Long> STORE_SCRIPT = RedisScript.of("""
            redis.call('DEL', KEYS[1])
            redis.call('HSET', KEYS[1],
                'codeDigest', ARGV[1],
                'expiresAt', ARGV[2],
                'remainingAttempts', ARGV[3])
            redis.call('PEXPIREAT', KEYS[1], ARGV[4])
            return tonumber(ARGV[2])
            """, Long.class);

    /**
     * 原子校验并消费脚本。
     *
     * <p>{@code KEYS[1]} 是完整 Redis key；{@code ARGV[1]} 是待校验验证码摘要；{@code ARGV[2]}
     * 是校验时间的 epoch milliseconds。返回码依次表示：{@code 0} 不存在、{@code 1} 已过期、{@code 2}
     * 成功、{@code 3} 不匹配但仍可尝试、{@code 4} 尝试次数耗尽、{@code 5} Hash 数据不完整。</p>
     *
     * <p>成功、过期和次数耗尽都会删除 key；普通不匹配会原子递减 {@code remainingAttempts}。</p>
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
    private final String applicationName;

    /**
     * 使用应用名称隔离 Redis 数据，创建验证码存储。
     *
     * @param redisTemplate Redis 字符串操作模板
     * @param secret 至少 32 字节的共享 HMAC 密钥
     * @param expiredRetention 业务过期后的保留时间
     * @param applicationName Redis key 和摘要使用的应用名称
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当密钥过短、保留时间不是正数或应用名称格式无效时
     */
    public RedisVerificationStore(
            StringRedisTemplate redisTemplate, byte[] secret, Duration expiredRetention, String applicationName) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        this.expiredRetention = Objects.requireNonNull(expiredRetention, "expiredRetention must not be null");
        this.applicationName = Objects.requireNonNull(applicationName, "applicationName must not be null");
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("secret must contain at least 32 bytes");
        }
        if (expiredRetention.isZero() || expiredRetention.isNegative()) {
            throw new IllegalArgumentException("expiredRetention must be positive: " + expiredRetention);
        }
        this.secret = secret.clone();
        if (!APPLICATION_NAME_PATTERN.matcher(applicationName).matches()) {
            throw new IllegalArgumentException(
                    "applicationName must start with an alphanumeric character and contain only letters, digits, '.', '_', or '-': "
                            + applicationName);
        }
    }

    /**
     * 保存验证码摘要、过期时间和最大校验次数。
     *
     * @param key 渠道隔离的验证码存储键
     * @param code 验证码明文，仅用于计算摘要
     * @param policy 验证码策略
     * @param issuedAt 签发时间
     * @return 存储结果
     * @throws VerificationStoreException 当 Redis 操作失败时
     */
    @Override
    public StoreResult store(VerificationStoreKey key, String code, VerificationPolicy policy, Instant issuedAt)
            throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Instant expiresAt = issuedAt.plus(policy.ttl());
        Instant deleteAt = expiresAt.plus(expiredRetention);
        Long result = execute(
                STORE_SCRIPT,
                redisKey(key),
                codeDigest(key, code),
                Long.toString(expiresAt.toEpochMilli()),
                Integer.toString(policy.maxAttempts()),
                Long.toString(deleteAt.toEpochMilli()));
        if (result == null) {
            throw new VerificationStoreException("Redis store script returned no result");
        }
        return new StoreResult(Instant.ofEpochMilli(result));
    }

    /**
     * 原子校验并消费验证码。
     *
     * @param key 渠道隔离的验证码存储键
     * @param code 待校验的验证码
     * @param verifiedAt 校验时间
     * @return 验证结果
     * @throws VerificationStoreException 当 Redis 操作失败时
     */
    @Override
    public VerifyResult verifyAndConsume(VerificationStoreKey key, String code, Instant verifiedAt)
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
            case 0 -> VerifyResult.NOT_FOUND;
            case 1 -> VerifyResult.EXPIRED;
            case 2 -> VerifyResult.SUCCESS;
            case 3 -> VerifyResult.MISMATCH;
            case 4 -> VerifyResult.ATTEMPTS_EXHAUSTED;
            case 5 -> throw new VerificationStoreException("Redis verification state is incomplete");
            default -> throw new VerificationStoreException("Redis verify script returned an unknown status");
        };
    }

    /**
     * 当验证码键和验证码同时匹配时删除记录。
     *
     * @param key 渠道隔离的验证码存储键
     * @param code 待失效的验证码
     * @return 是否删除了记录
     * @throws VerificationStoreException 当 Redis 操作失败时
     */
    @Override
    public boolean invalidate(VerificationStoreKey key, String code) throws VerificationStoreException {
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
     * @param key 包含渠道、业务域、用途和验证主体的验证码存储键
     * @return 包含业务前缀和验证主体摘要的 Redis key
     */
    private String redisKey(VerificationStoreKey key) {
        String prefix = applicationName + ":verification:" + STORAGE_VERSION + ':';
        return prefix
                + key.channel().value()
                + ':'
                + key.key().namespace()
                + ':'
                + key.key().purpose()
                + ':'
                + digest(KEY_DIGEST_DOMAIN, key, null);
    }

    /**
     * 创建与业务维度和验证主体绑定的验证码摘要。
     *
     * <p>相同验证码用于不同 subject、namespace 或 purpose 时会生成不同摘要。</p>
     *
     * @param key 验证码键
     * @param code 验证码明文，仅在当前进程中用于计算摘要
     * @return 无填充 Base64URL 编码的 HMAC-SHA256 摘要
     */
    private String codeDigest(VerificationStoreKey key, String code) {
        return digest(CODE_DIGEST_DOMAIN, key, code);
    }

    /**
     * 按确定的分段编码计算 HMAC-SHA256 摘要。
     *
     * <p>{@code domain} 用于隔离 Redis key 摘要和验证码摘要，防止相同输入在不同用途间复用。各字符串段通过
     * {@link #update(Mac, String)} 编码，顺序为 domain、applicationName、namespace、purpose、subject，
     * 以及可选 code。</p>
     *
     * @param domain 摘要用途
     * @param key 验证码键
     * @param code 可选验证码；生成 Redis key 摘要时为 {@code null}
     * @return 无填充 Base64URL 编码摘要
     * @throws VerificationStoreException 当运行环境不支持 HmacSHA256 时
     */
    private String digest(String domain, VerificationStoreKey key, @Nullable String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, domain);
            update(mac, applicationName);
            update(mac, key.channel().value());
            update(mac, key.key().namespace());
            update(mac, key.key().purpose());
            update(mac, key.key().subject());
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
     * @param mac 当前 HMAC 计算器
     * @param value 要写入的字符串段
     */
    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    /**
     * 使用单个 Redis key 执行 Lua 脚本并统一转换 Spring Data Redis 访问异常。
     *
     * @param script 要执行的脚本
     * @param key 唯一的 Redis key
     * @param arguments 脚本参数
     * @param <T> 脚本返回类型
     * @return 脚本结果，Redis 未返回值时为 {@code null}
     * @throws VerificationStoreException 当 Redis 访问失败时
     */
    @SuppressWarnings("DataFlowIssue")
    private <T extends @Nullable Object> @Nullable T execute(RedisScript<T> script, String key, String... arguments) {
        try {
            return redisTemplate.execute(script, List.of(key), (Object[]) arguments);
        } catch (DataAccessException exception) {
            throw new VerificationStoreException("Redis verification operation failed", exception);
        }
    }
}
