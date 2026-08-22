package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class EmailVerificationApplicationService {

    private static final String NAMESPACE = "account";
    private static final String PURPOSE = "email-verification";

    private final EmailVerificationService verificationService;

    EmailVerificationApplicationService(EmailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    Instant issue(String email) {
        return switch (verificationService.issue(key(email))) {
            case IssueResult.Accepted accepted -> accepted.expiresAt();
            case IssueResult.Uncertain uncertain -> uncertain.expiresAt();
            case IssueResult.Throttled throttled -> throw new VerificationThrottledException(throttled.retryAfter());
        };
    }

    void verify(String email, String code) {
        VerifyResult result = verificationService.verify(key(email), code);
        if (result != VerifyResult.SUCCESS) {
            throw new InvalidVerificationCodeException();
        }
    }

    private VerificationKey key(String email) {
        Objects.requireNonNull(email, "email must not be null");
        return new VerificationKey(NAMESPACE, PURPOSE, email.strip().toLowerCase(Locale.ROOT));
    }
}
