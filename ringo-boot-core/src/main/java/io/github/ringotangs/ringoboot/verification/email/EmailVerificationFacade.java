package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * 为业务用例提供开箱即用的邮箱验证码签发和校验能力。
 *
 * <p>Provides business use cases with ready-to-use email verification issuance and validation.</p>
 *
 * @apiNote 邮箱会去除首尾空白并按 {@link Locale#ROOT} 转为小写。校验成功会一次性消费验证码，登录、绑定等
 *     业务应在同一用例中立即继续后续操作。 / Email addresses are stripped and lower-cased with
 *     {@link Locale#ROOT}. Successful verification consumes the code once, so login, binding, and
 *     similar use cases should immediately continue their business operation in the same use case.
 */
public final class EmailVerificationFacade {

    private final EmailVerificationService verificationService;

    /**
     * 创建邮箱验证码 Facade。
     *
     * <p>Creates an email verification facade.</p>
     *
     * @param verificationService 底层邮箱验证码服务 / underlying email verification service
     */
    public EmailVerificationFacade(EmailVerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /**
     * 签发并派发邮箱验证码。
     *
     * <p>Issues and delivers an email verification code.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param email 目标邮箱 / destination email address
     * @return 验证码过期时间 / code expiration instant
     * @throws VerificationThrottledException 当重发间隔尚未结束时 / if the resend interval has not elapsed
     */
    public Instant issue(String namespace, String purpose, String email) {
        return switch (verificationService.issue(key(namespace, purpose, email))) {
            case DeliveryResult.Delivered delivered -> delivered.expiresAt();
            case DeliveryResult.Throttled throttled -> throw new VerificationThrottledException(throttled.retryAfter());
        };
    }

    /**
     * 校验并一次性消费邮箱验证码。
     *
     * <p>Verifies and consumes an email verification code once.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param email 目标邮箱 / destination email address
     * @param code 待校验验证码 / verification code to check
     * @throws InvalidVerificationCodeException 当验证码未成功通过校验时 / if verification is unsuccessful
     */
    public void verify(String namespace, String purpose, String email, String code) {
        VerificationResult result = verificationService.verify(key(namespace, purpose, email), code);
        if (result != VerificationResult.SUCCESS) {
            throw new InvalidVerificationCodeException();
        }
    }

    private VerificationKey key(String namespace, String purpose, String email) {
        Objects.requireNonNull(email, "email must not be null");
        return new VerificationKey(namespace, purpose, email.strip().toLowerCase(Locale.ROOT));
    }
}
