package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.InMemoryIssueRateLimitStore;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitManager;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitRule;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeDeliveryRejectedException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 统一编排验证码生成、存储、渠道派发、失败补偿和校验消费流程。
 *
 *
 * <p><strong>API 注意事项：</strong> 子类只需实现 {@link #dispatch(CodeDelivery)} 以完成邮件、短信等渠道的派发。
 */
public abstract class AbstractVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final IssueRateLimiter issueRateLimiter;
    private final VerificationPolicy defaultPolicy;
    private final Clock clock;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建渠道服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @throws NullPointerException 当生成器或存储为 {@code null} 时
     */
    protected AbstractVerificationService(CodeGenerator codeGenerator, VerificationStore store) {
        this(codeGenerator, store, defaultIssueRateLimiter(), VerificationPolicy.defaults(), Clock.systemUTC());
    }

    /**
     * 使用指定默认策略和 UTC 系统时钟创建渠道服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param defaultPolicy 默认验证码策略
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy) {
        this(codeGenerator, store, defaultIssueRateLimiter(), defaultPolicy, Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、默认策略和时钟创建渠道服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param defaultPolicy 默认验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy, Clock clock) {
        this(codeGenerator, store, defaultIssueRateLimiter(), defaultPolicy, clock);
    }

    /**
     * 使用指定生成器、存储、签发限流器、默认策略和时钟创建渠道服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param defaultPolicy 默认验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy defaultPolicy,
            Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.issueRateLimiter = Objects.requireNonNull(issueRateLimiter, "issueRateLimiter must not be null");
        this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public final IssueResult issue(VerificationKey key)
            throws CodeGenerationException, CodeSenderException, IssueRateLimitException, VerificationStoreException {
        return issue(key, defaultPolicy);
    }

    /** {@inheritDoc} */
    @Override
    public final IssueResult issue(VerificationKey key, VerificationPolicy policy)
            throws CodeGenerationException, CodeSenderException, IssueRateLimitException, VerificationStoreException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Instant issuedAt = clock.instant();
        IssueLimitResult limitResult = Objects.requireNonNull(
                issueRateLimiter.acquire(key, issuedAt), "issue rate limiter result must not be null");
        if (limitResult instanceof IssueLimitResult.Throttled throttled) {
            return new IssueResult.Throttled(throttled.retryAfter());
        }
        String code = codeGenerator.generate(policy.length());
        validateGeneratedCode(code, policy.length());
        StoreResult stored = store.store(key, code, policy, issuedAt);
        return dispatchStored(key, code, stored.expiresAt());
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
     *
     * @param delivery 验证码交付内容
     * @return 渠道对发送请求的受理结果
     * @throws CodeSenderException 当渠道派发操作失败时
     */
    protected abstract CodeSendResult dispatch(CodeDelivery delivery) throws CodeSenderException;

    private IssueResult dispatchStored(VerificationKey key, String code, Instant expiresAt) {
        try {
            CodeSendResult result = Objects.requireNonNull(
                    dispatch(new CodeDelivery(key, code, expiresAt)), "code sender result must not be null");
            return switch (result) {
                case ACCEPTED -> new IssueResult.Accepted(expiresAt);
                case UNKNOWN -> new IssueResult.Uncertain(expiresAt);
                case REJECTED -> throw new CodeDeliveryRejectedException();
            };
        } catch (RuntimeException dispatchFailure) {
            invalidateAfterFailure(key, code, dispatchFailure);
            throw dispatchFailure;
        }
    }

    private void validateGeneratedCode(String code, int expectedLength) {
        //noinspection ConstantValue -- 第三方生成器可能在运行时违反 SPI 契约。
        if (code == null || code.isBlank() || code.length() != expectedLength) {
            throw new CodeGenerationException("generated code must be non-blank and have length " + expectedLength);
        }
    }

    private void invalidateAfterFailure(VerificationKey key, String code, RuntimeException dispatchFailure) {
        try {
            store.invalidate(key, code);
        } catch (RuntimeException invalidationFailure) {
            dispatchFailure.addSuppressed(invalidationFailure);
        }
    }

    private static IssueRateLimiter defaultIssueRateLimiter() {
        IssueRateLimitRule rule = IssueRateLimitRule.of(
                "default-key-cooldown",
                context -> IssueLimitBucket.of(
                        context.key().namespace(),
                        context.key().purpose(),
                        context.key().subject()),
                1,
                Duration.ofSeconds(60));
        return new IssueRateLimitManager(List.of(rule), new InMemoryIssueRateLimitStore());
    }
}
