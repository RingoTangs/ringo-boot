package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Result of issuing a verification code. */
public sealed interface IssueResult permits IssueResult.Issued, IssueResult.Throttled {

    /** A newly issued plaintext code. Callers should deliver and discard it promptly. */
    record Issued(String code, Instant expiresAt) implements IssueResult {

        public Issued {
            Objects.requireNonNull(code, "code must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        @Override
        public String toString() {
            return "Issued[code=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }

    /** Issuance was rejected until the resend interval elapses. */
    record Throttled(Duration retryAfter) implements IssueResult {

        public Throttled {
            Objects.requireNonNull(retryAfter, "retryAfter must not be null");
            if (retryAfter.isNegative()) {
                throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
            }
        }
    }
}
