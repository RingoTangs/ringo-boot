package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void dispatchesCompleteDeliveryToEmailSender() {
        VerificationKey key = new VerificationKey("account", "login", "user@example.com");
        AtomicReference<EmailCodeDelivery> captured = new AtomicReference<>();
        AtomicReference<VerificationChannel> capturedChannel = new AtomicReference<>();
        EmailVerificationService service = new EmailVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                (context, requestedAt) -> {
                    capturedChannel.set(context.channel());
                    return new io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Allowed();
                },
                VerificationPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                delivery -> {
                    captured.set(delivery);
                    return CodeSendResult.ACCEPTED;
                });

        IssueResult.Accepted result = assertInstanceOf(IssueResult.Accepted.class, service.issue(key));

        assertEquals("account", captured.get().namespace());
        assertEquals(VerificationChannel.EMAIL, capturedChannel.get());
        assertEquals("login", captured.get().purpose());
        assertEquals("user@example.com", captured.get().email());
        assertEquals("123456", captured.get().code());
        assertEquals(result.expiresAt(), captured.get().expiresAt());
    }

    @Test
    void rejectsNullSender() {
        assertThrows(
                NullPointerException.class,
                () -> new EmailVerificationService(
                        length -> "123456",
                        new InMemoryVerificationStore(),
                        IssueRateLimiter.permitAll(),
                        VerificationPolicy.defaults(),
                        null));
    }

    @Test
    void exposesOnlyStandardAndClockAwareConstructors() {
        assertEquals(2, EmailVerificationService.class.getConstructors().length);
    }

    @Test
    void allowsContextCustomizationWithoutChangingEmailChannel() throws Exception {
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        CustomEmailVerificationService service = new CustomEmailVerificationService((context, requestedAt) -> {
            captured.set(context);
            return new io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Allowed();
        });

        service.issue(new VerificationKey("account", "login", "user@example.com"));

        assertEquals("tenant-1", captured.get().attribute("tenant-id").orElseThrow());
        assertEquals(VerificationChannel.EMAIL, captured.get().channel());
        assertTrue(Modifier.isFinal(
                EmailVerificationService.class.getDeclaredMethod("channel").getModifiers()));
    }

    private static final class CustomEmailVerificationService extends EmailVerificationService {

        private CustomEmailVerificationService(IssueRateLimiter limiter) {
            super(
                    length -> "123456",
                    new InMemoryVerificationStore(),
                    limiter,
                    VerificationPolicy.defaults(),
                    delivery -> CodeSendResult.ACCEPTED);
        }

        @Override
        protected IssueContext customizeIssueContext(IssueContext context) {
            return context.with("tenant-id", "tenant-1");
        }
    }
}
