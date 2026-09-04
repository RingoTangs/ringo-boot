package io.github.ringotangs.ringoboot.verification.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryVerificationStoreTest {

    @Test
    void expiresAndRemovesCodeAtExpirationBoundary() {
        VerificationStoreKey key = new VerificationStoreKey(
                new VerificationKey("account", "login", "user@example.com"), VerificationChannel.EMAIL);
        VerificationPolicy policy = new VerificationPolicy(6, Duration.ofMinutes(5), 5);
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryVerificationStore store = new InMemoryVerificationStore();
        store.store(key, "123456", policy, issuedAt);

        assertEquals(VerifyResult.EXPIRED, store.verifyAndConsume(key, "123456", issuedAt.plus(policy.ttl())));
        assertEquals(VerifyResult.NOT_FOUND, store.verifyAndConsume(key, "123456", issuedAt.plus(policy.ttl())));
    }
}
