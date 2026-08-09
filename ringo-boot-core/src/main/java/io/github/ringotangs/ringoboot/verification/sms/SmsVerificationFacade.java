package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import java.time.Instant;
import java.util.Objects;

/**
 * 为业务用例提供开箱即用的短信验证码签发和校验能力。
 *
 * <p>Provides business use cases with ready-to-use SMS verification issuance and validation.</p>
 *
 * @apiNote 手机号只去除首尾空白，不推断或改写国家区号。校验成功会一次性消费验证码，登录、绑定等业务应在
 *     同一用例中立即继续后续操作。 / Phone numbers are only stripped; country codes are not
 *     inferred or rewritten. Successful verification consumes the code once, so login, binding,
 *     and similar use cases should immediately continue their business operation in the same use case.
 */
public final class SmsVerificationFacade {

    private final SmsVerificationService verificationService;

    /**
     * 创建短信验证码 Facade。
     *
     * <p>Creates an SMS verification facade.</p>
     *
     * @param verificationService 底层短信验证码服务 / underlying SMS verification service
     */
    public SmsVerificationFacade(SmsVerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /**
     * 签发并派发短信验证码。
     *
     * <p>Issues and delivers an SMS verification code.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param phoneNumber 目标手机号 / destination phone number
     * @return 验证码过期时间 / code expiration instant
     * @throws VerificationThrottledException 当重发间隔尚未结束时 / if the resend interval has not elapsed
     */
    public Instant issue(String namespace, String purpose, String phoneNumber) {
        return switch (verificationService.issue(key(namespace, purpose, phoneNumber))) {
            case DeliveryResult.Delivered delivered -> delivered.expiresAt();
            case DeliveryResult.Throttled throttled -> throw new VerificationThrottledException(throttled.retryAfter());
        };
    }

    /**
     * 校验并一次性消费短信验证码。
     *
     * <p>Verifies and consumes an SMS verification code once.</p>
     *
     * @param namespace 业务命名空间 / business namespace
     * @param purpose 验证用途 / verification purpose
     * @param phoneNumber 目标手机号 / destination phone number
     * @param code 待校验验证码 / verification code to check
     * @throws InvalidVerificationCodeException 当验证码未成功通过校验时 / if verification is unsuccessful
     */
    public void verify(String namespace, String purpose, String phoneNumber, String code) {
        VerificationResult result = verificationService.verify(key(namespace, purpose, phoneNumber), code);
        if (result != VerificationResult.SUCCESS) {
            throw new InvalidVerificationCodeException();
        }
    }

    private VerificationKey key(String namespace, String purpose, String phoneNumber) {
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        return new VerificationKey(namespace, purpose, phoneNumber.strip());
    }
}
