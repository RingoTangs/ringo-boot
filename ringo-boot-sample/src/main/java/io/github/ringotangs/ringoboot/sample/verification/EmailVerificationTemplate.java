package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.VerificationService;
import io.github.ringotangs.ringoboot.verification.VerificationTemplate;
import org.springframework.stereotype.Component;

/**
 * 使用邮件渠道派发验证码的流程模板。
 *
 * <p>Verification workflow template that dispatches codes through the email
 * channel.</p>
 */
@Component
final class EmailVerificationTemplate extends VerificationTemplate {

    private final EmailCodeSender emailCodeSender;

    /**
     * 使用验证码服务和邮件发送器创建模板。
     *
     * <p>Creates the template with the verification service and email sender.</p>
     *
     * @param verificationService 验证码生命周期服务 / the verification lifecycle service
     * @param emailCodeSender 邮件验证码发送器 / the email verification code sender
     */
    EmailVerificationTemplate(VerificationService verificationService, EmailCodeSender emailCodeSender) {
        super(verificationService);
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
