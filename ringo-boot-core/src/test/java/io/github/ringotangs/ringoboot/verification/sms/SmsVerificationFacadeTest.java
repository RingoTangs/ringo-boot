package io.github.ringotangs.ringoboot.verification.sms;

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

class SmsVerificationFacadeTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

    @Test
    void stripsPhoneNumberWithoutRewritingIt() {
        StubStore store = new StubStore();

        Instant expiresAt = facade(store).issue("account", "login", "  +86 13800000000  ");

        assertEquals(EXPIRES_AT, expiresAt);
        assertEquals(new VerificationKey("account", "login", "+86 13800000000"), store.lastKey);
    }

    @Test
    void convertsResultsToSymmetricBusinessExceptions() {
        StubStore store = new StubStore();
        store.storeResult = new StoreResult.Throttled(Duration.ofSeconds(4));
        VerificationThrottledException throttled = assertThrows(
                VerificationThrottledException.class, () -> facade(store).issue("account", "login", "+8613800000000"));
        assertEquals(Duration.ofSeconds(4), throttled.retryAfter());

        store.verificationResult = VerificationResult.EXPIRED;
        assertThrows(
                InvalidVerificationCodeException.class,
                () -> facade(store).verify("account", "login", "+8613800000000", "123456"));
    }

    private SmsVerificationFacade facade(StubStore store) {
        SmsVerificationService service = new SmsVerificationService(length -> "123456", store, delivery -> {});
        return new SmsVerificationFacade(service);
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
