package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SmsVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void dispatchesCompleteDeliveryToSmsSender() {
        VerificationKey key = new VerificationKey("account", "login", "+8613800000000");
        AtomicReference<SmsCodeDelivery> captured = new AtomicReference<>();
        SmsVerificationService service = new SmsVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                IssueRateLimiter.permitAll(),
                VerificationPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                delivery -> {
                    captured.set(delivery);
                    return CodeSendResult.ACCEPTED;
                });

        IssueResult.Accepted result = assertInstanceOf(IssueResult.Accepted.class, service.issue(key));

        assertEquals("account", captured.get().namespace());
        assertEquals("login", captured.get().purpose());
        assertEquals("+8613800000000", captured.get().phoneNumber());
        assertEquals("123456", captured.get().code());
        assertEquals(result.expiresAt(), captured.get().expiresAt());
    }

    @Test
    void rejectsNullSender() {
        assertThrows(
                NullPointerException.class,
                () -> new SmsVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        IssueRateLimiter.permitAll(),
                        VerificationPolicy.defaults(),
                        null));
    }

    @Test
    void exposesOnlyStandardAndClockAwareConstructors() {
        assertEquals(2, SmsVerificationService.class.getConstructors().length);
    }
}
