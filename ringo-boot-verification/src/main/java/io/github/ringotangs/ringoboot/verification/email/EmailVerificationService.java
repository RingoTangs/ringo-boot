package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.IssueContextResolver;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Clock;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并通过邮件渠道派发。
 *
 */
public final class EmailVerificationService extends AbstractVerificationService {

    private final EmailCodeSender sender;

    /**
     * 使用指定签发限流器、服务级验证码策略和 UTC 系统时钟创建邮件验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param issueRateLimiter 验证码签发限流器
     * @param issueContextResolver 签发上下文解析器
     * @param verificationPolicy 服务级验证码策略
     * @param sender 邮件发送器
     */
    public EmailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            IssueContextResolver issueContextResolver,
            VerificationPolicy verificationPolicy,
            EmailCodeSender sender) {
        this(
                codeGenerator,
                store,
                issueRateLimiter,
                issueContextResolver,
                verificationPolicy,
                Clock.systemUTC(),
                sender);
    }

    /**
     * 使用指定签发限流器、服务级验证码策略和时钟创建邮件验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param issueRateLimiter 验证码签发限流器
     * @param issueContextResolver 签发上下文解析器
     * @param verificationPolicy 服务级验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @param sender 邮件发送器
     */
    public EmailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            IssueContextResolver issueContextResolver,
            VerificationPolicy verificationPolicy,
            Clock clock,
            EmailCodeSender sender) {
        super(codeGenerator, store, issueRateLimiter, issueContextResolver, verificationPolicy, clock);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 将通用验证码交付内容转换为邮件发送内容并交给邮件发送器。
     *
     * @param delivery 通用验证码交付内容
     * @throws CodeSenderException 当邮件派发失败时
     */
    @Override
    protected CodeSendResult dispatch(CodeDelivery delivery) throws CodeSenderException {
        return sender.send(new EmailCodeDelivery(
                delivery.context().key().namespace(),
                delivery.context().key().purpose(),
                delivery.context().key().subject(),
                delivery.code(),
                delivery.expiresAt()));
    }

    @Override
    protected VerificationChannel channel() {
        return VerificationChannel.EMAIL;
    }
}
