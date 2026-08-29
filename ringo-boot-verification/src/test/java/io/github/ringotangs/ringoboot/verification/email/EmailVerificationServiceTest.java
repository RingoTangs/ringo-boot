package io.github.ringotangs.ringoboot.verification.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimiter;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EmailVerificationServiceTest {

    @Test
    void dispatchesCompleteContextToEmailSender() {
        VerificationKey key = new VerificationKey("account", "login", "user@example.com");
        VerificationPolicy policy = VerificationPolicy.defaults();
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        AtomicReference<String> capturedCode = new AtomicReference<>();
        AtomicReference<Instant> capturedExpiresAt = new AtomicReference<>();
        AtomicReference<VerificationChannel> capturedChannel = new AtomicReference<>();
        EmailVerificationService service = new EmailVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                (context, requestedAt) -> {
                    capturedChannel.set(context.channel());
                    return new io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Allowed();
                },
                policy,
                (context, code, expiresAt) -> {
                    captured.set(context);
                    capturedCode.set(code);
                    capturedExpiresAt.set(expiresAt);
                    return CodeSendResult.ACCEPTED;
                });

        IssueResult.Accepted result = assertInstanceOf(IssueResult.Accepted.class, service.issue(key));

        assertEquals("account", captured.get().key().namespace());
        assertEquals(VerificationChannel.EMAIL, capturedChannel.get());
        assertEquals("login", captured.get().key().purpose());
        assertEquals("user@example.com", captured.get().key().subject());
        assertEquals(policy, captured.get().policy());
        assertEquals("123456", capturedCode.get());
        assertEquals(result.expiresAt(), capturedExpiresAt.get());
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
    void exposesOnlyStandardConstructor() {
        var constructors = EmailVerificationService.class.getConstructors();

        assertEquals(2, constructors.length);
        assertDoesNotThrow(() -> EmailVerificationService.class.getConstructor(
                CodeGenerator.class,
                VerificationStore.class,
                IssueRateLimiter.class,
                VerificationPolicy.class,
                EmailCodeSender.class));
        assertDoesNotThrow(() -> EmailVerificationService.class.getConstructor(
                CodeGenerator.class,
                VerificationStore.class,
                IssueRateLimiter.class,
                VerificationPolicy.class,
                List.class,
                EmailCodeSender.class));
    }

    @Test
    void appliesContextContributorWithoutChangingEmailChannel() throws Exception {
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        AtomicReference<IssueContext> dispatched = new AtomicReference<>();
        EmailVerificationService service = new EmailVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                (context, requestedAt) -> {
                    captured.set(context);
                    return new io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Allowed();
                },
                VerificationPolicy.defaults(),
                List.of(context -> context.with("tenant-id", "tenant-1")),
                (context, code, expiresAt) -> {
                    dispatched.set(context);
                    return CodeSendResult.ACCEPTED;
                });

        service.issue(new VerificationKey("account", "login", "user@example.com"));

        assertEquals("tenant-1", captured.get().attribute("tenant-id").orElseThrow());
        assertEquals(VerificationChannel.EMAIL, captured.get().channel());
        assertEquals("tenant-1", dispatched.get().attribute("tenant-id").orElseThrow());
        assertEquals(VerificationPolicy.defaults(), dispatched.get().policy());
        assertTrue(Modifier.isFinal(
                EmailVerificationService.class.getDeclaredMethod("channel").getModifiers()));
    }
}
