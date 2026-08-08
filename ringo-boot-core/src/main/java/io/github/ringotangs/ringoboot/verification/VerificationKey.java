package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/** Identifies a verification by its purpose and subject. */
public record VerificationKey(String purpose, String subject) {

    public VerificationKey {
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }
}
