package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
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
 * 统一编排验证码生命周期并通过短信渠道派发。
 *
 */
public final class SmsVerificationService extends AbstractVerificationService {

    private final SmsCodeSender sender;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建短信验证服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param sender 短信发送器
     */
    public SmsVerificationService(CodeGenerator codeGenerator, VerificationStore store, SmsCodeSender sender) {
        super(codeGenerator, store);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和 UTC 系统时钟创建短信验证服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param policy 默认验证码策略
     * @param sender 短信发送器
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy policy, SmsCodeSender sender) {
        super(codeGenerator, store, policy);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和时钟创建短信验证服务。
     *
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param policy 默认验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @param sender 短信发送器
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            VerificationPolicy policy,
            Clock clock,
            SmsCodeSender sender) {
        super(codeGenerator, store, policy, clock);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定签发限流器、默认策略和 UTC 系统时钟创建短信验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param issueRateLimiter 验证码签发限流器
     * @param policy 默认验证码策略
     * @param sender 短信发送器
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy policy,
            SmsCodeSender sender) {
        this(codeGenerator, store, issueRateLimiter, policy, Clock.systemUTC(), sender);
    }

    /**
     * 使用指定签发限流器、默认策略和时钟创建短信验证服务。
     *
     * @param codeGenerator 验证码生成器
     * @param store 验证码存储
     * @param issueRateLimiter 验证码签发限流器
     * @param policy 默认验证码策略
     * @param clock 提供签发和校验时间的时钟
     * @param sender 短信发送器
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueRateLimiter issueRateLimiter,
            VerificationPolicy policy,
            Clock clock,
            SmsCodeSender sender) {
        super(codeGenerator, store, issueRateLimiter, policy, clock);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 将验证码交给短信发送器。
     *
     *
     * @param delivery 验证码交付内容
     * @throws CodeSenderException 当短信派发失败时
     */
    @Override
    protected CodeSendResult dispatch(CodeDelivery delivery) throws CodeSenderException {
        return sender.send(delivery);
    }
}
