package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationStore;
import org.springframework.stereotype.Component;

/**
 * 使用邮件渠道派发验证码的服务。
 *
 * <p>Verification service that dispatches codes through the email
 * channel.</p>
 */
@Component
final class EmailVerificationService extends AbstractVerificationService {

    private final EmailCodeSender emailCodeSender;

    /**
     * 使用验证码基础组件和邮件发送器创建服务。
     *
     * <p>Creates the service with the verification infrastructure and email sender.</p>
     *
     * @param codeGenerator 验证码生成器 / the code generator
     * @param store 验证码状态存储 / the verification state store
     * @param policy 默认验证码策略 / the default verification policy
     * @param emailCodeSender 邮件验证码发送器 / the email verification code sender
     */
    EmailVerificationService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            VerificationPolicy policy,
            EmailCodeSender emailCodeSender) {
        super(codeGenerator, store, policy);
        this.emailCodeSender = emailCodeSender;
    }

    /**
     * 将验证码派发到验证键指定的邮箱。
     *
     * <p>Dispatches the verification code to the email address identified by the
     * verification key.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    @Override
    protected void dispatch(CodeDelivery delivery) {
        emailCodeSender.send(delivery.key().subject(), delivery.code(), delivery.expiresAt());
    }
}
