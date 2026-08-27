package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Instant;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并通过邮件渠道派发。
 */
public class EmailVerificationService extends AbstractVerificationService {

    private final EmailCodeSender sender;

    /**
     * 使用指定签发限流器、服务级验证码策略和 UTC 系统时钟创建邮件验证服务。
     *
     * @param codeGenerator      验证码生成器
     * @param store              验证码存储
     * @param issueRateLimiter   验证码签发限流器
     * @param verificationPolicy 服务级验证码策略
     * @param sender             邮件发送器
     */
    public EmailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            EmailCodeSender sender) {
        super(codeGenerator, store, issueRateLimiter, verificationPolicy);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 根据签发上下文创建邮件消息并交给邮件发送器。
     *
     * @param context 当前签发流程的上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @throws CodeSenderException 当邮件派发失败时
     */
    @Override
    protected CodeSendResult dispatch(IssueContext context, String code, Instant expiresAt) throws CodeSenderException {
        return sender.send(EmailCodeMessage.from(context, code, expiresAt));
    }

    @Override
    protected final VerificationChannel channel() {
        return VerificationChannel.EMAIL;
    }
}
