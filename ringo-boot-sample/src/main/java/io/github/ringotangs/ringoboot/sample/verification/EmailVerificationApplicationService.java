package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.verification.email.EmailVerificationFacade;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
class EmailVerificationApplicationService {

    private static final String NAMESPACE = "account";
    private static final String PURPOSE = "email-verification";

    private final EmailVerificationFacade verificationFacade;

    EmailVerificationApplicationService(EmailVerificationFacade verificationFacade) {
        this.verificationFacade = verificationFacade;
    }

    Instant issue(String email) {
        return verificationFacade.issue(NAMESPACE, PURPOSE, email);
    }

    void verify(String email, String code) {
        verificationFacade.verify(NAMESPACE, PURPOSE, email, code);
    }
}
