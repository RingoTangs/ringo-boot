package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AbstractVerificationServiceTest {

    private static final VerificationKey LOGIN = new VerificationKey("login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issuesDispatchesAndVerifiesThroughServiceWithoutExposingCodeInResult() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        DeliveryResult.Delivered delivered = assertInstanceOf(DeliveryResult.Delivered.class, template.issue(LOGIN));

        assertEquals(NOW.plus(Duration.ofMinutes(5)), delivered.expiresAt());
        assertEquals(LOGIN, template.delivery().key());
        assertEquals("123456", template.delivery().code());
        assertFalse(template.delivery().toString().contains("123456"));
        assertFalse(delivered.toString().contains("123456"));
        assertEquals(VerificationResult.SUCCESS, template.verify(LOGIN, "123456"));
    }

    @Test
    void doesNotDispatchWhenIssuanceIsThrottled() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        template.issue(LOGIN);
        DeliveryResult.Throttled throttled = assertInstanceOf(DeliveryResult.Throttled.class, template.issue(LOGIN));

        assertEquals(1, template.dispatches());
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());
    }

    @Test
    void usesSuppliedPolicy() {
        AtomicInteger requestedLength = new AtomicInteger();
        CapturingVerificationService template = template(
                length -> {
                    requestedLength.set(length);
                    return "1234";
                },
                new InMemoryVerificationStore());
        VerificationPolicy policy = new VerificationPolicy(4, Duration.ofMinutes(1), 2, Duration.ZERO);

        DeliveryResult.Delivered delivered =
                assertInstanceOf(DeliveryResult.Delivered.class, template.issue(LOGIN, policy));

        assertEquals(4, requestedLength.get());
        assertEquals(NOW.plus(Duration.ofMinutes(1)), delivered.expiresAt());
    }

    @Test
    void invalidatesCodeWhenDispatchFailsAndAllowsImmediateRetry() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());
        CodeSenderException failure = new CodeSenderException("provider unavailable");
        template.failWith(failure);

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> template.issue(LOGIN));
        template.failWith(null);
        DeliveryResult result = template.issue(LOGIN);

        assertSame(failure, thrown);
        assertInstanceOf(DeliveryResult.Delivered.class, result);
    }

    @Test
    void preservesDispatchFailureAndSuppressesInvalidationFailure() {
        VerificationStoreException invalidationFailure = new VerificationStoreException("cleanup unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public boolean invalidate(VerificationKey key, String code) {
                throw invalidationFailure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);
        CodeSenderException dispatchFailure = new CodeSenderException("delivery unavailable");
        template.failWith(dispatchFailure);

        CodeSenderException thrown = assertThrows(CodeSenderException.class, () -> template.issue(LOGIN));

        assertSame(dispatchFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(invalidationFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void propagatesStoreFailureDuringIssuance() {
        VerificationStoreException failure = new VerificationStoreException("storage unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown = assertThrows(VerificationStoreException.class, () -> template.issue(LOGIN));

        assertSame(failure, thrown);
        assertEquals(0, template.dispatches());
    }

    @Test
    void propagatesStoreFailureDuringVerification() {
        VerificationStoreException failure = new VerificationStoreException("storage unavailable");
        VerificationStore store = new StubVerificationStore() {
            @Override
            public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
                throw failure;
            }
        };
        CapturingVerificationService template = template(length -> "123456", store);

        VerificationStoreException thrown =
                assertThrows(VerificationStoreException.class, () -> template.verify(LOGIN, "123456"));

        assertSame(failure, thrown);
    }

    @Test
    void validatesRequiredServiceArgumentsBeforeIssuance() {
        CapturingVerificationService template = template(length -> "123456", new InMemoryVerificationStore());

        assertThrows(
                NullPointerException.class,
                () -> new CapturingVerificationService(
                        null,
                        new InMemoryVerificationStore(),
                        VerificationPolicy.defaults(),
                        Clock.fixed(NOW, ZoneOffset.UTC)));
        assertThrows(NullPointerException.class, () -> template.issue(null));
        assertThrows(NullPointerException.class, () -> template.issue(LOGIN, null));
    }

    private CapturingVerificationService template(CodeGenerator generator, VerificationStore store) {
        return new CapturingVerificationService(
                generator, store, VerificationPolicy.defaults(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class CapturingVerificationService extends AbstractVerificationService {

        private final AtomicReference<CodeDelivery> delivery = new AtomicReference<>();
        private final AtomicInteger dispatches = new AtomicInteger();
        private RuntimeException failure;

        private CapturingVerificationService(
                CodeGenerator generator, VerificationStore store, VerificationPolicy policy, Clock clock) {
            super(generator, store, policy, clock);
        }

        @Override
        protected void dispatch(CodeDelivery delivery) {
            dispatches.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            this.delivery.set(delivery);
        }

        private CodeDelivery delivery() {
            return delivery.get();
        }

        private int dispatches() {
            return dispatches.get();
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }
    }

    private static class StubVerificationStore implements VerificationStore {

        @Override
        public StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt) {
            return new StoreResult.Stored(NOW.plusSeconds(60));
        }

        @Override
        public VerificationResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt) {
            return VerificationResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return true;
        }
    }
}
