package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Atomic storage contract for verification state. Implementations must not persist plaintext codes. */
public interface VerificationStore {

    StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt);

    VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt);

    /** Internal issuance decision returned to {@link VerificationService}. */
    sealed interface StoreResult permits StoreResult.Stored, StoreResult.Throttled {

        record Stored(Instant expiresAt) implements StoreResult {

            public Stored {
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");
            }
        }

        record Throttled(Duration retryAfter) implements StoreResult {

            public Throttled {
                Objects.requireNonNull(retryAfter, "retryAfter must not be null");
                if (retryAfter.isNegative()) {
                    throw new IllegalArgumentException("retryAfter must not be negative: " + retryAfter);
                }
            }
        }
    }
}
