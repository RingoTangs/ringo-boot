package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationStore;
import java.time.Clock;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并通过邮件渠道派发。
 *
 * <p>Coordinates the verification lifecycle and dispatches codes through email.</p>
 */
public final class EmailVerificationService extends AbstractVerificationService {

    private final EmailCodeSender sender;

    /**
     * 使用安全默认策略和 UTC 系统时钟创建邮件验证服务。
     *
     * <p>Creates an email verification service with secure defaults and the UTC system
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param sender 邮件发送器 / the email sender
     */
    public EmailVerificationService(CodeGenerator codeGenerator, VerificationStore store, EmailCodeSender sender) {
        super(codeGenerator, store);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和 UTC 系统时钟创建邮件验证服务。
     *
     * <p>Creates an email verification service with the supplied default policy and
     * UTC system clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param policy 默认验证码策略 / the default verification policy
     * @param sender 邮件发送器 / the email sender
     */
    public EmailVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy policy, EmailCodeSender sender) {
        super(codeGenerator, store, policy);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 使用指定默认策略和时钟创建邮件验证服务。
     *
     * <p>Creates an email verification service with the supplied default policy and
     * clock.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码存储 / the verification store
     * @param policy 默认验证码策略 / the default verification policy
     * @param clock 提供签发和校验时间的时钟 / the issuance and verification clock
     * @param sender 邮件发送器 / the email sender
     */
    public EmailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            VerificationPolicy policy,
            Clock clock,
            EmailCodeSender sender) {
        super(codeGenerator, store, policy, clock);
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * 将验证码交给邮件发送器。
     *
     * <p>Delegates the verification code delivery to the email sender.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    @Override
    protected void dispatch(CodeDelivery delivery) {
        sender.send(delivery);
    }
}
