package io.github.ringotangs.ringoboot.verification;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 统一编排验证码生成、存储、渠道派发、失败补偿和校验消费流程。
 *
 * <p>Coordinates verification code generation, storage, channel dispatch,
 * failure compensation, and verification consumption.</p>
 *
 * @apiNote 子类只需实现 {@link #dispatch(CodeDelivery)} 以完成邮件、短信等渠道的派发。 /
 *     Subclasses only implement {@link #dispatch(CodeDelivery)} for channels such as
 *     email or SMS.
 */
public abstract class AbstractVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final VerificationPolicy defaultPolicy;
    private final Clock clock;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建渠道服务。
     *
     * <p>Creates a channel service with the secure default policy and UTC system
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @throws NullPointerException 当生成器或存储为 {@code null} 时 / if the generator or store is {@code null}
     */
    protected AbstractVerificationService(CodeGenerator codeGenerator, VerificationStore store) {
        this(codeGenerator, store, VerificationPolicy.defaults(), Clock.systemUTC());
    }

    /**
     * 使用指定默认策略和 UTC 系统时钟创建渠道服务。
     *
     * <p>Creates a channel service with the supplied default policy and UTC system
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @param defaultPolicy 默认验证码策略 / the default verification policy
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy) {
        this(codeGenerator, store, defaultPolicy, Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、默认策略和时钟创建渠道服务。
     *
     * <p>Creates a channel service with the supplied generator, store, default policy,
     * and clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @param defaultPolicy 默认验证码策略 / the default verification policy
     * @param clock 提供签发和校验时间的时钟 / the clock supplying issuance and verification instants
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy, Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public final DeliveryResult issue(VerificationKey key) throws VerificationStoreException {
        return issue(key, defaultPolicy);
    }

    /** {@inheritDoc} */
    @Override
    public final DeliveryResult issue(VerificationKey key, VerificationPolicy policy)
            throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        String code =
                Objects.requireNonNull(codeGenerator.generate(policy.length()), "generated code must not be null");
        if (code.isBlank() || code.length() != policy.length()) {
            throw new IllegalStateException("generated code must be non-blank and have length " + policy.length());
        }
        Instant issuedAt = clock.instant();
        return switch (store.store(key, code, policy, issuedAt)) {
            case StoreResult.Throttled throttled -> new DeliveryResult.Throttled(throttled.retryAfter());
            case StoreResult.Stored stored -> dispatchStored(key, code, stored.expiresAt());
        };
    }

    /** {@inheritDoc} */
    @Override
    public final VerificationResult verify(VerificationKey key, String code) throws VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return store.verifyAndConsume(key, code, clock.instant());
    }

    /**
     * 将已生成并存储的验证码派发到具体渠道。
     *
     * <p>Dispatches a generated and stored verification code through the concrete
     * channel.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    protected abstract void dispatch(CodeDelivery delivery);

    private DeliveryResult dispatchStored(VerificationKey key, String code, Instant expiresAt) {
        try {
            dispatch(new CodeDelivery(key, code, expiresAt));
        } catch (RuntimeException dispatchFailure) {
            invalidateAfterFailure(key, code, dispatchFailure);
            throw dispatchFailure;
        }
        return new DeliveryResult.Delivered(expiresAt);
    }

    private void invalidateAfterFailure(VerificationKey key, String code, RuntimeException dispatchFailure) {
        try {
            store.invalidate(key, code);
        } catch (RuntimeException invalidationFailure) {
            dispatchFailure.addSuppressed(invalidationFailure);
        }
    }
}
