package io.github.ringotangs.ringoboot.verification;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Thread-safe in-memory verification storage for tests, local development, and
 * single-instance applications. State is not shared or durable.
 */
public final class InMemoryVerificationStore implements VerificationStore {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;

    private final ConcurrentMap<VerificationKey, Entry> entries = new ConcurrentHashMap<>();
    private final byte[] secret;

    public InMemoryVerificationStore() {
        this(new SecureRandom());
    }

    public InMemoryVerificationStore(SecureRandom random) {
        Objects.requireNonNull(random, "random must not be null");
        this.secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
    }

    @Override
    public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        AtomicReference<StoreResult> result = new AtomicReference<>();
        entries.compute(key, (ignored, existing) -> {
            if (existing != null && issuedAt.isBefore(existing.expiresAt()) && issuedAt.isBefore(existing.resendAt())) {
                result.set(new StoreResult.Throttled(Duration.between(issuedAt, existing.resendAt())));
                return existing;
            }
            Instant expiresAt = issuedAt.plus(policy.ttl());
            result.set(new StoreResult.Stored(expiresAt));
            return new Entry(
                    digest(key, code), expiresAt, issuedAt.plus(policy.resendInterval()), policy.maxAttempts());
        });
        return Objects.requireNonNull(result.get());
    }

    @Override
    public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        AtomicReference<VerificationResult> result = new AtomicReference<>(VerificationResult.NOT_FOUND);
        byte[] candidateDigest = digest(key, code);
        entries.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return null;
            }
            if (!verifiedAt.isBefore(existing.expiresAt())) {
                result.set(VerificationResult.EXPIRED);
                return null;
            }
            if (MessageDigest.isEqual(existing.digest(), candidateDigest)) {
                result.set(VerificationResult.SUCCESS);
                return null;
            }
            int remainingAttempts = existing.remainingAttempts() - 1;
            if (remainingAttempts <= 0) {
                result.set(VerificationResult.ATTEMPTS_EXHAUSTED);
                return null;
            }
            result.set(VerificationResult.MISMATCH);
            return existing.withRemainingAttempts(remainingAttempts);
        });
        return result.get();
    }

    private byte[] digest(VerificationKey key, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            update(mac, key.purpose());
            update(mac, key.subject());
            update(mac, code);
            return mac.doFinal();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    private void update(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }

    private record Entry(byte[] digest, Instant expiresAt, Instant resendAt, int remainingAttempts) {

        Entry withRemainingAttempts(int attempts) {
            return new Entry(digest, expiresAt, resendAt, attempts);
        }
    }
}
