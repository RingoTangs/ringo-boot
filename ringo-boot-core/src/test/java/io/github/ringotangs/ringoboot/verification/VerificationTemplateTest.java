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

class VerificationTemplateTest {

    private static final VerificationKey LOGIN = new VerificationKey("login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issuesDispatchesAndVerifiesThroughServiceWithoutExposingCodeInResult() {
        CapturingTemplate template = template(length -> "123456", new InMemoryVerificationStore());

        DeliveryResult.Delivered delivered = assertInstanceOf(DeliveryResult.Delivered.class, template.issue(LOGIN));

        assertEquals(NOW.plus(Duration.ofMinutes(5)), delivered.expiresAt());
        assertEquals(LOGIN, template.delivery().key());
        assertEquals("123456", template.delivery().code());
        assertFalse(template.delivery().toString().contains("123456"));
        assertFalse(delivered.toString().contains("123456"));
        assertEquals(VerificationResult.SUCCESS, template.service().verify(LOGIN, "123456"));
    }

    @Test
    void doesNotDispatchWhenIssuanceIsThrottled() {
        CapturingTemplate template = template(length -> "123456", new InMemoryVerificationStore());

        template.issue(LOGIN);
        DeliveryResult.Throttled throttled = assertInstanceOf(DeliveryResult.Throttled.class, template.issue(LOGIN));

        assertEquals(1, template.dispatches());
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());
    }

    @Test
    void usesSuppliedPolicy() {
        AtomicInteger requestedLength = new AtomicInteger();
        CapturingTemplate template = template(
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
        CapturingTemplate template = template(length -> "123456", new InMemoryVerificationStore());
        IllegalStateException failure = new IllegalStateException("provider unavailable");
        template.failWith(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> template.issue(LOGIN));
        template.failWith(null);
        DeliveryResult result = template.issue(LOGIN);

        assertSame(failure, thrown);
        assertInstanceOf(DeliveryResult.Delivered.class, result);
    }

    @Test
    void preservesDispatchFailureAndSuppressesInvalidationFailure() {
        IllegalStateException invalidationFailure = new IllegalStateException("cleanup unavailable");
        VerificationService service = new StubVerificationService() {
            @Override
            public boolean invalidate(VerificationKey key, String code) {
                throw invalidationFailure;
            }
        };
        CapturingTemplate template = new CapturingTemplate(service);
        IllegalArgumentException dispatchFailure = new IllegalArgumentException("delivery unavailable");
        template.failWith(dispatchFailure);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> template.issue(LOGIN));

        assertSame(dispatchFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(invalidationFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void validatesRequiredTemplateArgumentsBeforeIssuance() {
        CapturingTemplate template = template(length -> "123456", new InMemoryVerificationStore());

        assertThrows(NullPointerException.class, () -> new CapturingTemplate(null));
        assertThrows(NullPointerException.class, () -> template.issue(null));
        assertThrows(NullPointerException.class, () -> template.issue(LOGIN, null));
    }

    private CapturingTemplate template(CodeGenerator generator, VerificationStore store) {
        VerificationService service = new DefaultVerificationService(
                generator, store, VerificationPolicy.defaults(), Clock.fixed(NOW, ZoneOffset.UTC));
        return new CapturingTemplate(service);
    }

    private static final class CapturingTemplate extends VerificationTemplate {

        private final VerificationService service;
        private final AtomicReference<CodeDelivery> delivery = new AtomicReference<>();
        private final AtomicInteger dispatches = new AtomicInteger();
        private RuntimeException failure;

        private CapturingTemplate(VerificationService service) {
            super(service);
            this.service = service;
        }

        @Override
        protected void dispatch(CodeDelivery delivery) {
            dispatches.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            this.delivery.set(delivery);
        }

        private VerificationService service() {
            return service;
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

    private static class StubVerificationService implements VerificationService {

        @Override
        public IssueResult issue(VerificationKey key) {
            return new IssueResult.Issued("123456", NOW.plusSeconds(60));
        }

        @Override
        public IssueResult issue(VerificationKey key, VerificationPolicy policy) {
            return issue(key);
        }

        @Override
        public VerificationResult verify(VerificationKey key, String code) {
            return VerificationResult.NOT_FOUND;
        }

        @Override
        public boolean invalidate(VerificationKey key, String code) {
            return true;
        }
    }
}
