package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.context.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.context.IssueContextValidator;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 统一编排验证码生成、存储、渠道派发、失败补偿和校验消费流程。
 *
 * <p><strong>API 注意事项：</strong> 子类通过 {@link #channel()} 声明渠道，实现 {@link #completeIssue(IssueContext, String, Instant)}
 * 完成渠道发送或同步渲染；
 * 请求级属性通过 {@link IssueContextManager} 统一提供。
 *
 * @param <R> 渠道成功签发后的结果类型
 */
public abstract class AbstractVerificationService<R> implements VerificationService<R> {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final IssueLimiter issueLimiter;
    private final VerificationPolicy verificationPolicy;
    private final IssueContextManager issueContextManager;
    private final Clock clock = Clock.systemUTC();

    /**
     * 使用完整依赖和 UTC 系统时钟创建渠道服务。
     *
     * @param codeGenerator       验证码生成器
     * @param store               验证码状态存储
     * @param issueLimiter    验证码签发限流器
     * @param verificationPolicy  服务级验证码策略
     * @param issueContextManager 统一准备最终签发上下文的 Manager
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueLimiter issueLimiter,
            VerificationPolicy verificationPolicy,
            IssueContextManager issueContextManager) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.issueLimiter = Objects.requireNonNull(issueLimiter, "issueLimiter must not be null");
        this.verificationPolicy = Objects.requireNonNull(verificationPolicy, "verificationPolicy must not be null");
        this.issueContextManager = Objects.requireNonNull(issueContextManager, "issueContextManager must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final R issue(VerificationKey key) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        VerificationChannel channel = Objects.requireNonNull(channel(), "verification channel must not be null");
        IssueContext baseContext = IssueContext.of(key, channel, verificationPolicy);
        IssueContext context = Objects.requireNonNull(
                issueContextManager.enrich(baseContext), "issue context manager result must not be null");
        IssueContextValidator.requirePreservedContext(baseContext, context, "issue context manager");
        Instant requestedAt = clock.instant();
        IssueLimitResult limitResult = Objects.requireNonNull(
                issueLimiter.acquire(context, requestedAt), "issue limiter result must not be null");
        if (limitResult instanceof IssueLimitResult.Throttled(List<IssueLimitViolation> violations)) {
            throw new IssueLimitExceededException(violations);
        }
        int codeLength = verificationPolicy.length();
        String code = codeGenerator.generate(codeLength);
        //noinspection ConstantValue -- 第三方生成器可能在运行时违反 SPI 契约。
        if (code == null || code.isBlank() || code.length() != codeLength) {
            throw new CodeGenerationException("generated code must be non-blank and have length " + codeLength);
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = Objects.requireNonNull(
                store.store(storeKey(context), code, verificationPolicy, issuedAt),
                "verification store expiration must not be null");
        return completeStoredIssue(context, code, expiresAt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void verify(VerificationKey key, String code) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        VerificationChannel channel = Objects.requireNonNull(channel(), "verification channel must not be null");
        VerifyResult result = Objects.requireNonNull(
                store.verifyAndConsume(new VerificationStoreKey(key, channel), code, clock.instant()),
                "verification store result must not be null");
        if (result != VerifyResult.SUCCESS) {
            throw new VerificationFailedException(result);
        }
    }

    /**
     * 完成已生成并存储验证码的渠道发送或同步渲染。
     *
     * <p>正常返回必须非空；运行时异常会触发按存储键和验证码条件撤销。
     * 邮件、短信接受状态不确定时应正常返回 DeliveryResult.Uncertain，保留验证码。
     * 发送或渲染耗时计入验证码有效期。
     *
     * @param context   当前签发流程的上下文
     * @param code      仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 渠道对应的签发结果
     * @throws VerificationException 当渠道发送或渲染失败时
     */
    protected abstract R completeIssue(IssueContext context, String code, Instant expiresAt)
            throws VerificationException;

    /**
     * 返回当前服务使用的验证码渠道。
     *
     * @return 稳定的验证码渠道
     */
    protected abstract VerificationChannel channel();

    private R completeStoredIssue(IssueContext context, String code, Instant expiresAt) {
        try {
            return Objects.requireNonNull(
                    completeIssue(context, code, expiresAt), "issue completion result must not be null");
        } catch (RuntimeException completionFailure) {
            try {
                store.invalidate(storeKey(context), code);
            } catch (RuntimeException invalidationFailure) {
                completionFailure.addSuppressed(invalidationFailure);
            }
            throw completionFailure;
        }
    }

    private VerificationStoreKey storeKey(IssueContext context) {
        return new VerificationStoreKey(context.key(), context.channel());
    }
}
