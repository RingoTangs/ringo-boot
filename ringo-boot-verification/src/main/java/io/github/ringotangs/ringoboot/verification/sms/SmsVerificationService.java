package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Instant;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并通过短信渠道派发。
 */
public class SmsVerificationService extends AbstractVerificationService {

    private final SmsCodeSender sender;

    /**
     * 使用完整依赖创建短信验证服务。
     *
     * @param codeGenerator       验证码生成器
     * @param store               验证码存储
     * @param issueRateLimiter    验证码签发限流器
     * @param verificationPolicy  服务级验证码策略
     * @param issueContextManager 统一准备最终签发上下文的 Manager
     * @param sender              短信发送器
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy verificationPolicy,
            IssueContextManager issueContextManager,
            SmsCodeSender sender) {
        super(codeGenerator, store, issueRateLimiter, verificationPolicy, issueContextManager);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 根据签发上下文创建短信消息并交给短信发送器。
     *
     * @param context 当前签发流程的上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @throws CodeSenderException 当短信派发失败时
     */
    @Override
    protected CodeSendResult dispatch(IssueContext context, String code, Instant expiresAt) throws CodeSenderException {
        return sender.send(context, code, expiresAt);
    }

    @Override
    protected final VerificationChannel channel() {
        return VerificationChannel.SMS;
    }
}
