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
 * 请求级属性通过 {@link IssueContextContributor} 组合提供。
 */
public abstract class AbstractVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final IssueRateLimiter issueRateLimiter;
    private final VerificationPolicy verificationPolicy;
    private final List<IssueContextContributor> contextContributors;
    private final Clock clock;

    /**
     * 使用指定生成器、存储、签发限流器、服务级验证码策略和 UTC 系统时钟创建渠道服务。
     *
     * @param codeGenerator      验证码生成器
     * @param store              验证码状态存储
     * @param issueRateLimiter   验证码签发限流器
     * @param verificationPolicy 服务级验证码策略
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy) {
        this(codeGenerator, store, issueRateLimiter, verificationPolicy, List.of(), Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、签发限流器、服务级策略、上下文贡献器和 UTC 系统时钟创建渠道服务。
     *
     * @param codeGenerator       验证码生成器
     * @param store               验证码状态存储
     * @param issueRateLimiter    验证码签发限流器
     * @param verificationPolicy  服务级验证码策略
     * @param contextContributors 按顺序补充签发上下文的贡献器
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            List<IssueContextContributor> contextContributors) {
        this(codeGenerator, store, issueRateLimiter, verificationPolicy, contextContributors, Clock.systemUTC());
    }

    /**
     * 使用指定生成器、存储、签发限流器、服务级验证码策略和时钟创建渠道服务，供需要控制时间的子类使用。
     *
     * @param codeGenerator      验证码生成器
     * @param store              验证码状态存储
     * @param issueRateLimiter   验证码签发限流器
     * @param verificationPolicy 服务级验证码策略
     * @param clock              提供签发和校验时间的时钟
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            Clock clock) {
        this(codeGenerator, store, issueRateLimiter, verificationPolicy, List.of(), clock);
    }

    /**
     * 使用完整依赖和指定时钟创建渠道服务。
     *
     * @param codeGenerator       验证码生成器
     * @param store               验证码状态存储
     * @param issueRateLimiter    验证码签发限流器
     * @param verificationPolicy  服务级验证码策略
     * @param contextContributors 按顺序补充签发上下文的贡献器
     * @param clock               提供签发和校验时间的时钟
     */
    protected AbstractVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            List<IssueContextContributor> contextContributors,
            Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.issueRateLimiter = Objects.requireNonNull(issueRateLimiter, "issueRateLimiter must not be null");
        this.verificationPolicy = Objects.requireNonNull(verificationPolicy, "verificationPolicy must not be null");
        Objects.requireNonNull(contextContributors, "contextContributors must not be null");
        this.contextContributors = List.copyOf(contextContributors);
        this.contextContributors.forEach(
                contributor -> Objects.requireNonNull(contributor, "context contributor must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IssueResult issue(VerificationKey key) throws VerificationException {
        Objects.requireNonNull(key, "key must not be null");
        VerificationChannel channel = Objects.requireNonNull(channel(), "verification channel must not be null");
        IssueContext context = IssueContext.of(key, channel, verificationPolicy);
        for (int index = 0; index < contextContributors.size(); index++) {
            IssueContextContributor contributor = contextContributors.get(index);
            IssueContext contributed = Objects.requireNonNull(
                    contributor.contribute(context), "issue context contributor result must not be null: " + index);
            context = requireEnrichedContext(context, contributed, "issue context contributor at index " + index);
        }
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

    private static void requirePreservedContext(IssueContext expected, IssueContext actual, String source) {
        if (!actual.key().equals(expected.key())) {
            throw new IllegalArgumentException(source + " must preserve the verification key");
        }
        if (!actual.channel().equals(expected.channel())) {
            throw new IllegalArgumentException(source + " must preserve the verification channel");
        }
        if (!actual.policy().equals(expected.policy())) {
            throw new IllegalArgumentException(source + " must preserve the verification policy");
        }
    }

    private static IssueContext requireEnrichedContext(IssueContext expected, IssueContext actual, String source) {
        requirePreservedContext(expected, actual, source);
        expected.attributes().forEach((name, value) -> {
            if (!value.equals(actual.attributes().get(name))) {
                throw new IllegalArgumentException(source + " must preserve existing issue context attribute: " + name);
            }
        });
        return actual;
    }
}
