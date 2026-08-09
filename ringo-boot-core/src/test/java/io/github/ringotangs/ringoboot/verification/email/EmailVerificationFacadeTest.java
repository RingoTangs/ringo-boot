package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailVerificationFacadeTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void normalizesEmailAndReturnsExpiration() {
        StubStore store = new StubStore();
        DefaultEmailVerificationFacade facade = facade(store);

        Instant expiresAt = facade.issue("account", "bind-email", "  USER@Example.COM  ");

        assertEquals(EXPIRES_AT, expiresAt);
        assertEquals(new VerificationKey("account", "bind-email", "user@example.com"), store.lastKey);
    }

    @Test
    void convertsThrottlingToBusinessException() {
        StubStore store = new StubStore();
        store.storeResult = new StoreResult.Throttled(Duration.ofSeconds(9));

        VerificationThrottledException exception = assertThrows(
                VerificationThrottledException.class,
                () -> facade(store).issue("account", "login", "user@example.com"));

        assertEquals(Duration.ofSeconds(9), exception.retryAfter());
    }

    @Test
    void acceptsSuccessAndHidesEveryUnsuccessfulResult() {
        StubStore store = new StubStore();
        DefaultEmailVerificationFacade facade = facade(store);
        store.verificationResult = VerificationResult.SUCCESS;
        assertDoesNotThrow(() -> facade.verify("account", "login", "user@example.com", "123456"));

        for (VerificationResult result : VerificationResult.values()) {
            if (result == VerificationResult.SUCCESS) {
                continue;
            }
            store.verificationResult = result;
            assertThrows(
                    InvalidVerificationCodeException.class,
                    () -> facade.verify("account", "login", "user@example.com", "123456"));
        }
    }

    private DefaultEmailVerificationFacade facade(StubStore store) {
        EmailVerificationService service = new EmailVerificationService(length -> "123456", store, delivery -> {});
        return new DefaultEmailVerificationFacade(service);
    }

    private static final class StubStore implements VerificationStore {

        private StoreResult storeResult = new StoreResult.Stored(EXPIRES_AT);
        private VerificationResult verificationResult = VerificationResult.SUCCESS;
        private VerificationKey lastKey;

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            lastKey = key;
            return storeResult;
        }

        @Override
        public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            lastKey = key;
            return verificationResult;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return true;
        }
    }
}
