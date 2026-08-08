package io.github.ringotangs.ringoboot.verification;

import java.time.Duration;
import java.util.Objects;

/** Controls code generation and verification lifecycle limits. */
public record VerificationPolicy(int length, Duration ttl, int maxAttempts, Duration resendInterval) {

    private static final int DEFAULT_LENGTH = 6;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final Duration DEFAULT_RESEND_INTERVAL = Duration.ofSeconds(60);

    public VerificationPolicy {
        Objects.requireNonNull(ttl, "ttl must not be null");
        Objects.requireNonNull(resendInterval, "resendInterval must not be null");
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0: " + length);
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than 0: " + maxAttempts);
        }
        if (resendInterval.isNegative()) {
            throw new IllegalArgumentException("resendInterval must not be negative: " + resendInterval);
        }
    }

    /** Returns the secure defaults used by {@link VerificationService#issue(VerificationKey)}. */
    public static VerificationPolicy defaults() {
        return new VerificationPolicy(DEFAULT_LENGTH, DEFAULT_TTL, DEFAULT_MAX_ATTEMPTS, DEFAULT_RESEND_INTERVAL);
    }
}
