package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Clock;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并通过短信渠道派发。
 *
 * <p>Coordinates the verification lifecycle and dispatches codes through SMS.</p>
 */
public final class SmsVerificationService extends AbstractVerificationService {

    private final SmsCodeSender sender;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建短信验证服务。
     *
     * <p>Creates an SMS verification service with secure defaults and the UTC system
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param sender 短信发送器 / the SMS sender
     */
    public SmsVerificationService(CodeGenerator codeGenerator, VerificationStore store, SmsCodeSender sender) {
        super(codeGenerator, store);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和 UTC 系统时钟创建短信验证服务。
     *
     * <p>Creates an SMS verification service with the supplied default policy and UTC
     * system clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param policy 默认验证码策略 / the default verification policy
     * @param sender 短信发送器 / the SMS sender
     */
    public SmsVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy policy, SmsCodeSender sender) {
        super(codeGenerator, store, policy);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和时钟创建短信验证服务。
     *
     * <p>Creates an SMS verification service with the supplied default policy and
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param policy 默认验证码策略 / the default verification policy
     * @param clock 提供签发和校验时间的时钟 / the issuance and verification clock
     * @param sender 短信发送器 / the SMS sender
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
     * 将验证码交给短信发送器。
     *
     * <p>Delegates the verification code delivery to the SMS sender.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     * @throws CodeSenderException 当短信派发失败时 / if SMS delivery fails
     */
    @Override
    protected void dispatch(CodeDelivery delivery) throws CodeSenderException {
        sender.send(delivery);
    }
}
