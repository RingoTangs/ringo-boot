package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeDeliveryRejectedException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Clock;
import java.time.Instant;
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
    private final VerificationPolicy verificationPolicy;
    private final Clock clock;

    /**
     * 使用默认验证码策略、不限制签发的限流器和 UTC 系统时钟创建渠道服务。
     *
     * <p>选择此构造器表示调用方明确不执行签发限流。需要限制签发频率时，应使用接收 {@link IssueRateLimiter} 的构造器。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @throws NullPointerException 当生成器或存储为 {@code null} 时
     */
    protected AbstractVerificationService(CodeGenerator codeGenerator, VerificationStore store) {
        this(codeGenerator, store, IssueRateLimiter.permitAll(), VerificationPolicy.defaults(), Clock.systemUTC());
    }

    /**
     * 使用指定服务级验证码策略、不限制签发的限流器和 UTC 系统时钟创建渠道服务。
     *
     * <p>选择此构造器表示调用方明确不执行签发限流。需要限制签发频率时，应使用接收 {@link IssueRateLimiter} 的构造器。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param verificationPolicy 服务级验证码策略
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy verificationPolicy) {
        this(codeGenerator, store, IssueRateLimiter.permitAll(), verificationPolicy, Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、服务级验证码策略、时钟和不限制签发的限流器创建渠道服务。
     *
     * <p>选择此构造器表示调用方明确不执行签发限流。需要限制签发频率时，应使用接收 {@link IssueRateLimiter} 的构造器。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param verificationPolicy 服务级验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy verificationPolicy, Clock clock) {
        this(codeGenerator, store, IssueRateLimiter.permitAll(), verificationPolicy, clock);
    }

    /**
     * 使用指定生成器、存储、签发限流器、服务级验证码策略和时钟创建渠道服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码状态存储
     * @param issueRateLimiter 验证码签发限流器
     * @param verificationPolicy 服务级验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.issueRateLimiter = Objects.requireNonNull(issueRateLimiter, "issueRateLimiter must not be null");
        this.verificationPolicy = Objects.requireNonNull(verificationPolicy, "verificationPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public final IssueResult issue(VerificationKey key) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        Instant issuedAt = clock.instant();
        IssueLimitResult limitResult = Objects.requireNonNull(
                issueRateLimiter.acquire(key, issuedAt), "issue rate limiter result must not be null");
        if (limitResult instanceof IssueLimitResult.Throttled(java.time.Duration retryAfter)) {
            return new IssueResult.Throttled(retryAfter);
        }
        int codeLength = verificationPolicy.length();
        String code = codeGenerator.generate(codeLength);
        //noinspection ConstantValue -- 第三方生成器可能在运行时违反 SPI 契约。
        if (code == null || code.isBlank() || code.length() != codeLength) {
            throw new CodeGenerationException("generated code must be non-blank and have length " + codeLength);
        }
        StoreResult stored = store.store(key, code, verificationPolicy, issuedAt);
        return dispatchStoredCode(key, code, stored.expiresAt());
    }

    /** {@inheritDoc} */
    @Override
    public final VerifyResult verify(VerificationKey key, String code) throws VerificationException {
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

    private IssueResult dispatchStoredCode(VerificationKey key, String code, Instant expiresAt) {
        try {
            CodeSendResult result = Objects.requireNonNull(
                    dispatch(new CodeDelivery(key, code, expiresAt)), "code sender result must not be null");
            return switch (result) {
                case ACCEPTED -> new IssueResult.Accepted(expiresAt);
                case UNKNOWN -> new IssueResult.Uncertain(expiresAt);
                case REJECTED -> throw new CodeDeliveryRejectedException();
            };
        } catch (RuntimeException dispatchFailure) {
            try {
                store.invalidate(key, code);
            } catch (RuntimeException invalidationFailure) {
                dispatchFailure.addSuppressed(invalidationFailure);
            }
            throw dispatchFailure;
        }
    }
}
