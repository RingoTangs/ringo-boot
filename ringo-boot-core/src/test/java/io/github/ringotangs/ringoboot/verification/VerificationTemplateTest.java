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
    void issuesDeliversAndVerifiesWithoutExposingCodeInResult() {
        VerificationTemplate template = template(length -> "123456", new InMemoryVerificationStore());
        AtomicReference<CodeDelivery> captured = new AtomicReference<>();

        DeliveryResult.Delivered delivered =
                assertInstanceOf(DeliveryResult.Delivered.class, template.issue(LOGIN, captured::set));

        assertEquals(NOW.plus(Duration.ofMinutes(5)), delivered.expiresAt());
        assertEquals(LOGIN, captured.get().key());
        assertEquals("123456", captured.get().code());
        assertFalse(captured.get().toString().contains("123456"));
        assertFalse(delivered.toString().contains("123456"));
        assertEquals(VerificationResult.SUCCESS, template.verify(LOGIN, "123456"));
    }

    @Test
    void doesNotInvokeSenderWhenIssuanceIsThrottled() {
        VerificationTemplate template = template(length -> "123456", new InMemoryVerificationStore());
        AtomicInteger deliveries = new AtomicInteger();
        CodeSender sender = ignored -> deliveries.incrementAndGet();

        template.issue(LOGIN, sender);
        DeliveryResult.Throttled throttled =
                assertInstanceOf(DeliveryResult.Throttled.class, template.issue(LOGIN, sender));

        assertEquals(1, deliveries.get());
        assertEquals(Duration.ofSeconds(60), throttled.retryAfter());
    }

    @Test
    void usesSuppliedPolicy() {
        AtomicInteger requestedLength = new AtomicInteger();
        VerificationTemplate template = template(
                length -> {
                    requestedLength.set(length);
                    return "1234";
                },
                new InMemoryVerificationStore());
        VerificationPolicy policy = new VerificationPolicy(4, Duration.ofMinutes(1), 2, Duration.ZERO);

        DeliveryResult.Delivered delivered =
                assertInstanceOf(DeliveryResult.Delivered.class, template.issue(LOGIN, policy, ignored -> {}));

        assertEquals(4, requestedLength.get());
        assertEquals(NOW.plus(Duration.ofMinutes(1)), delivered.expiresAt());
    }

    @Test
    void invalidatesCodeWhenDeliveryFailsAndAllowsImmediateRetry() {
        VerificationTemplate template = template(length -> "123456", new InMemoryVerificationStore());
        IllegalStateException failure = new IllegalStateException("provider unavailable");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> template.issue(LOGIN, ignored -> {
                    throw failure;
                }));
        DeliveryResult result = template.issue(LOGIN, ignored -> {});

        assertSame(failure, thrown);
        assertInstanceOf(DeliveryResult.Delivered.class, result);
    }

    @Test
    void preservesDeliveryFailureAndSuppressesInvalidationFailure() {
        IllegalStateException invalidationFailure = new IllegalStateException("cleanup unavailable");
        VerificationService service = new StubVerificationService() {
            @Override
            public boolean invalidate(VerificationKey key, String code) {
                throw invalidationFailure;
            }
        };
        VerificationTemplate template = new VerificationTemplate(service);
        IllegalArgumentException deliveryFailure = new IllegalArgumentException("delivery unavailable");

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> template.issue(LOGIN, ignored -> {
                    throw deliveryFailure;
                }));

        assertSame(deliveryFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(invalidationFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void validatesRequiredTemplateArgumentsBeforeIssuance() {
        VerificationTemplate template = template(length -> "123456", new InMemoryVerificationStore());

        assertThrows(NullPointerException.class, () -> new VerificationTemplate(null));
        assertThrows(NullPointerException.class, () -> template.issue(null, ignored -> {}));
        assertThrows(NullPointerException.class, () -> template.issue(LOGIN, null));
        assertThrows(NullPointerException.class, () -> template.issue(LOGIN, (VerificationPolicy) null, ignored -> {}));
    }

    private VerificationTemplate template(CodeGenerator generator, VerificationStore store) {
        VerificationService service = new DefaultVerificationService(
                generator, store, VerificationPolicy.defaults(), Clock.fixed(NOW, ZoneOffset.UTC));
        return new VerificationTemplate(service);
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
