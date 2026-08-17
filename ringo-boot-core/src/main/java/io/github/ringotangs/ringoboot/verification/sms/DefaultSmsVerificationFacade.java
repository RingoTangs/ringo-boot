package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import java.time.Instant;
import java.util.Objects;

/**
 * 基于 {@link SmsVerificationService} 的默认短信验证码门面。
 *
 *
 * <p><strong>API 注意事项：</strong> 手机号只去除首尾空白，不推断或改写国家区号。校验成功会一次性消费验证码。
 */
public final class DefaultSmsVerificationFacade implements SmsVerificationFacade {

    private final SmsVerificationService verificationService;

    /**
     * 使用底层短信验证码服务创建默认门面。
     *
     *
     * @param verificationService 底层短信验证码服务
     */
    public DefaultSmsVerificationFacade(SmsVerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public Instant issue(String namespace, String purpose, String phoneNumber) {
        return switch (verificationService.issue(key(namespace, purpose, phoneNumber))) {
            case IssueResult.Accepted accepted -> accepted.expiresAt();
            case IssueResult.Uncertain uncertain -> uncertain.expiresAt();
            case IssueResult.Throttled throttled -> throw new VerificationThrottledException(throttled.retryAfter());
        };
    }

    /** {@inheritDoc} */
    @Override
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
