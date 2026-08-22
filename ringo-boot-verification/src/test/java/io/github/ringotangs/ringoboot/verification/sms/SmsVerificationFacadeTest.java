package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationThrottledException;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
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
        VerificationThrottledException throttled = assertThrows(
                VerificationThrottledException.class,
                () -> facade(store, new IssueLimitResult.Throttled(Duration.ofSeconds(4)), CodeSendResult.ACCEPTED)
                        .issue("account", "login", "+8613800000000"));
        assertEquals(Duration.ofSeconds(4), throttled.retryAfter());

        store.verificationResult = VerifyResult.EXPIRED;
        assertThrows(
                InvalidVerificationCodeException.class,
                () -> facade(store).verify("account", "login", "+8613800000000", "123456"));
    }

    @Test
    void treatsUnknownProviderOutcomeAsAcceptedByTheApplicationFacade() {
        StubStore store = new StubStore();

        Instant expiresAt = facade(store, CodeSendResult.UNKNOWN).issue("account", "login", "+8613800000000");

        assertEquals(EXPIRES_AT, expiresAt);
    }

    private DefaultSmsVerificationFacade facade(StubStore store) {
        return facade(store, CodeSendResult.ACCEPTED);
    }

    private DefaultSmsVerificationFacade facade(StubStore store, CodeSendResult sendResult) {
        return facade(store, new IssueLimitResult.Allowed(), sendResult);
    }

    private DefaultSmsVerificationFacade facade(
            StubStore store, IssueLimitResult limitResult, CodeSendResult sendResult) {
        SmsVerificationService service = new SmsVerificationService(
                length -> "123456",
                store,
                (key, requestedAt) -> limitResult,
                VerificationPolicy.defaults(),
                delivery -> sendResult);
        return new DefaultSmsVerificationFacade(service);
    }

    private static final class StubStore implements VerificationStore {

        private VerifyResult verificationResult = VerifyResult.SUCCESS;
        private VerificationKey lastKey;

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            lastKey = key;
            return new StoreResult(EXPIRES_AT);
        }

        @Override
        public VerifyResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            lastKey = key;
            return verificationResult;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return true;
        }
    }
}
