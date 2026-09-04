package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.channel.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.channel.email.EmailVerificationService;
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
            case DeliveryResult.Accepted accepted -> accepted.expiresAt();
            case DeliveryResult.Uncertain uncertain -> uncertain.expiresAt();
        };
    }

    void verify(String email, String code) {
        verificationService.verify(key(email), code);
    }

    private VerificationKey key(String email) {
        Objects.requireNonNull(email, "email must not be null");
        return new VerificationKey(NAMESPACE, PURPOSE, email.strip().toLowerCase(Locale.ROOT));
    }
}
