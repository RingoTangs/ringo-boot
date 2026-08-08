package io.github.ringotangs.ringoboot.verification;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 协调验证码生成器、原子存储、默认策略和时钟的默认验证服务实现。
 *
 * <p>Default verification service that coordinates code generation, atomic storage,
 * the default policy, and a clock.</p>
 */
public final class DefaultVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final VerificationPolicy defaultPolicy;
    private final Clock clock;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建验证服务。
     *
     * <p>Creates a verification service with the secure default policy and the UTC
     * system clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @throws NullPointerException 当生成器或存储为 {@code null} 时 / if the generator or store is
     *     {@code null}
     */
    public DefaultVerificationService(CodeGenerator codeGenerator, VerificationStore store) {
        this(codeGenerator, store, VerificationPolicy.defaults(), Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、默认策略和时钟创建验证服务。
     *
     * <p>Creates a verification service with the supplied generator, store, default
     * policy, and clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @param defaultPolicy {@link #issue(VerificationKey)} 使用的默认策略 / the default policy used by
     *     {@link #issue(VerificationKey)}
     * @param clock 提供签发和校验时间的时钟 / the clock that supplies issuance and verification instants
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    public DefaultVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy, Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 使用默认策略生成验证码并请求存储层原子签发。
     *
     * <p>Generates a code with the default policy and asks the store to issue it
     * atomically.</p>
     *
     * @param key 验证码键 / the verification key
     * @return 签发结果 / the issuance result
     * @throws NullPointerException 当验证码键为 {@code null} 时 / if the verification key is {@code null}
     * @throws IllegalStateException 当生成器返回 {@code null}、空白或长度错误的验证码时 / if the generator
     *     returns a {@code null}, blank, or incorrectly sized code
     */
    @Override
    public IssueResult issue(VerificationKey key) {
        return issue(key, defaultPolicy);
    }

    /**
     * 使用指定策略生成验证码并请求存储层原子签发。
     *
     * <p>Generates a code with the supplied policy and asks the store to issue it
     * atomically.</p>
     *
     * @param key 验证码键 / the verification key
     * @param policy 验证码策略 / the verification policy
     * @return 签发结果 / the issuance result
     * @throws NullPointerException 当验证码键或策略为 {@code null} 时 / if the key or policy is {@code null}
     * @throws IllegalStateException 当生成器返回 {@code null}、空白或长度错误的验证码时 / if the generator
     *     returns a {@code null}, blank, or incorrectly sized code
     */
    @Override
    public IssueResult issue(VerificationKey key, VerificationPolicy policy) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        String code =
                Objects.requireNonNull(codeGenerator.generate(policy.length()), "generated code must not be null");
        if (code.isBlank() || code.length() != policy.length()) {
            throw new IllegalStateException("generated code must be non-blank and have length " + policy.length());
        }
        Instant issuedAt = clock.instant();
        return switch (store.store(key, code, policy, issuedAt)) {
            case StoreResult.Stored stored -> new IssueResult.Issued(code, stored.expiresAt());
            case StoreResult.Throttled throttled -> new IssueResult.Throttled(throttled.retryAfter());
        };
    }

    /**
     * 使用当前时钟时间请求存储层原子校验并消费验证码。
     *
     * <p>Asks the store to atomically verify and consume the code at the current clock
     * instant.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待校验的验证码 / the code to verify
     * @return 校验结果 / the verification result
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时 / if the key or code is {@code null}
     */
    @Override
    public VerificationResult verify(VerificationKey key, String code) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return store.verifyAndConsume(key, code, clock.instant());
    }

    /**
     * 请求存储层仅在验证码匹配时原子地使记录失效。
     *
     * <p>Asks the store to atomically invalidate the record only when the code
     * matches.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待失效的验证码 / the code to invalidate
     * @return 是否删除了匹配记录 / whether a matching record was removed
     */
    @Override
    public boolean invalidate(VerificationKey key, String code) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return store.invalidate(key, code);
    }
}
