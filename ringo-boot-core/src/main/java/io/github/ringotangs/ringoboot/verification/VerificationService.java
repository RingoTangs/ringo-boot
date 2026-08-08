package io.github.ringotangs.ringoboot.verification;

/** Issues and verifies short-lived, one-time codes. */
public interface VerificationService {

    IssueResult issue(VerificationKey key);

    IssueResult issue(VerificationKey key, VerificationPolicy policy);

    VerificationResult verify(VerificationKey key, String code);
}
