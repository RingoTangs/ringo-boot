package io.github.ringotangs.ringoboot.verification;

/** Outcome of a verification attempt. */
public enum VerificationResult {
    SUCCESS,
    NOT_FOUND,
    EXPIRED,
    MISMATCH,
    ATTEMPTS_EXHAUSTED
}
