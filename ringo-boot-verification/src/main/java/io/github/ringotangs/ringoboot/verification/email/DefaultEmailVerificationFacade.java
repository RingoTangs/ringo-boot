package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * 基于 {@link EmailVerificationService} 的默认邮箱验证码门面。
 *
 *
 * <p><strong>API 注意事项：</strong> 邮箱会去除首尾空白并按 {@link Locale#ROOT} 转为小写。校验成功会一次性消费验证码。
 */
public final class DefaultEmailVerificationFacade implements EmailVerificationFacade {

    private final EmailVerificationService verificationService;

    /**
     * 使用底层邮箱验证码服务创建默认门面。
     *
     *
     * @param verificationService 底层邮箱验证码服务
     */
    public DefaultEmailVerificationFacade(EmailVerificationService verificationService) {
        this.verificationService = Objects.requireNonNull(verificationService, "verificationService must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public Instant issue(String namespace, String purpose, String email) {
        return switch (verificationService.issue(key(namespace, purpose, email))) {
            case IssueResult.Accepted accepted -> accepted.expiresAt();
            case IssueResult.Uncertain uncertain -> uncertain.expiresAt();
            case IssueResult.Throttled throttled -> throw new VerificationThrottledException(throttled.retryAfter());
        };
    }

    /** {@inheritDoc} */
    @Override
    public void verify(String namespace, String purpose, String email, String code) {
        VerifyResult result = verificationService.verify(key(namespace, purpose, email), code);
        if (result != VerifyResult.SUCCESS) {
            throw new InvalidVerificationCodeException();
        }
    }

    private VerificationKey key(String namespace, String purpose, String email) {
        Objects.requireNonNull(email, "email must not be null");
        return new VerificationKey(namespace, purpose, email.strip().toLowerCase(Locale.ROOT));
    }
}
