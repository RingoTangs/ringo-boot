package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 统一编排验证码生成、存储、渠道派发、失败补偿和校验消费流程。
 *
 * <p><strong>API 注意事项：</strong> 子类通过 {@link #channel()} 声明渠道，实现 {@link #dispatch(IssueContext, String, Instant)} 完成派发；
 * 请求级属性通过 {@link IssueContextManager} 统一提供。
 */
public abstract class AbstractVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final IssueRateLimiter issueRateLimiter;
    private final VerificationPolicy verificationPolicy;
    private final IssueContextManager issueContextManager;
    private final Clock clock = Clock.systemUTC();

    /**
     * 使用完整依赖和 UTC 系统时钟创建渠道服务。
     *
     * @param codeGenerator       验证码生成器
     * @param store               验证码状态存储
     * @param issueRateLimiter    验证码签发限流器
     * @param verificationPolicy  服务级验证码策略
     * @param issueContextManager 统一准备最终签发上下文的 Manager
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            IssueContextManager issueContextManager) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.issueRateLimiter = Objects.requireNonNull(issueRateLimiter, "issueRateLimiter must not be null");
        this.verificationPolicy = Objects.requireNonNull(verificationPolicy, "verificationPolicy must not be null");
        this.issueContextManager = Objects.requireNonNull(issueContextManager, "issueContextManager must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IssueResult issue(VerificationKey key) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        VerificationChannel channel = Objects.requireNonNull(channel(), "verification channel must not be null");
        IssueContext baseContext = IssueContext.of(key, channel, verificationPolicy);
        IssueContext context = Objects.requireNonNull(
                issueContextManager.enrich(baseContext), "issue context manager result must not be null");
        IssueContextValidator.requirePreservedContext(baseContext, context, "issue context manager");
        Instant issuedAt = clock.instant();
        IssueLimitResult limitResult = Objects.requireNonNull(
                issueRateLimiter.acquire(context, issuedAt), "issue rate limiter result must not be null");
        if (limitResult instanceof IssueLimitResult.Throttled(List<IssueLimitViolation> violations)) {
            return new IssueResult.Throttled(violations);
        }
        int codeLength = verificationPolicy.length();
        String code = codeGenerator.generate(codeLength);
        //noinspection ConstantValue -- 第三方生成器可能在运行时违反 SPI 契约。
        if (code == null || code.isBlank() || code.length() != codeLength) {
            throw new CodeGenerationException("generated code must be non-blank and have length " + codeLength);
        }
        StoreResult stored = store.store(context.key(), code, verificationPolicy, issuedAt);
        return dispatchStoredCode(context, code, stored.expiresAt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final VerifyResult verify(VerificationKey key, String code) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return store.verifyAndConsume(key, code, clock.instant());
    }

    /**
     * 将已生成并存储的验证码派发到具体渠道。
     *
     * @param context   当前签发流程的上下文
     * @param code      仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 渠道对发送请求的受理结果
     * @throws CodeSenderException 当渠道派发操作失败时
     */
    protected abstract CodeSendResult dispatch(IssueContext context, String code, Instant expiresAt)
            throws CodeSenderException;

    /**
     * 返回当前服务使用的验证码渠道。
     *
     * @return 稳定的验证码渠道
     */
    protected abstract VerificationChannel channel();

    private IssueResult dispatchStoredCode(IssueContext context, String code, Instant expiresAt) {
        try {
            CodeSendResult result =
                    Objects.requireNonNull(dispatch(context, code, expiresAt), "code sender result must not be null");
            return switch (result) {
                case ACCEPTED -> new IssueResult.Accepted(expiresAt);
                case UNKNOWN -> new IssueResult.Uncertain(expiresAt);
                case REJECTED -> throw new CodeDeliveryRejectedException(context.channel());
            };
        } catch (RuntimeException dispatchFailure) {
            try {
                store.invalidate(context.key(), code);
            } catch (RuntimeException invalidationFailure) {
                dispatchFailure.addSuppressed(invalidationFailure);
            }
            throw dispatchFailure;
        }
    }
}
