package io.github.ringotangs.ringoboot.sample.verification;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.verification.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.email.EmailVerificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
class EmailVerificationApplicationService {

    private static final String PURPOSE = "email-verification";

    private final EmailVerificationService verificationService;
    private final InMemoryEmailCodeSender testInbox;

    EmailVerificationApplicationService(
            EmailVerificationService verificationService, InMemoryEmailCodeSender testInbox) {
        this.verificationService = verificationService;
        this.testInbox = testInbox;
    }

    Instant issue(String email) {
        String normalizedEmail = normalize(email);
        return switch (verificationService.issue(key(normalizedEmail))) {
            case DeliveryResult.Delivered delivered -> delivered.expiresAt();
            case DeliveryResult.Throttled throttled ->
                throw ProblemException.withArguments(
                        VerificationProblemType.THROTTLED, retryAfterSeconds(throttled.retryAfter()));
        };
    }

    void verify(String email, String code) {
        VerificationResult result = verificationService.verify(key(normalize(email)), code);
        if (result != VerificationResult.SUCCESS) {
            throw new ProblemException(VerificationProblemType.INVALID_CODE);
        }
    }

    InMemoryEmailCodeSender.EmailCodeMessage findLatestTestMessage(String email) {
        return testInbox
                .findLatest(normalize(email))
                .orElseThrow(() -> new ProblemException(VerificationProblemType.TEST_MESSAGE_NOT_FOUND));
    }

    private VerificationKey key(String email) {
        return new VerificationKey(PURPOSE, email);
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private long retryAfterSeconds(Duration retryAfter) {
        long seconds = retryAfter.toSeconds();
        return retryAfter.minusSeconds(seconds).isZero() ? seconds : seconds + 1;
    }
}
