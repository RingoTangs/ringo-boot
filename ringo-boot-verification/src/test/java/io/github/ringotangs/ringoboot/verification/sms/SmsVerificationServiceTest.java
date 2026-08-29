package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SmsVerificationServiceTest {

    @Test
    void dispatchesCompleteContextToSmsSender() {
        VerificationKey key = new VerificationKey("account", "login", "+8613800000000");
        VerificationPolicy policy = VerificationPolicy.defaults();
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        AtomicReference<String> capturedCode = new AtomicReference<>();
        AtomicReference<Instant> capturedExpiresAt = new AtomicReference<>();
        AtomicReference<VerificationChannel> capturedChannel = new AtomicReference<>();
        SmsVerificationService service = new SmsVerificationService(
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
        assertEquals(VerificationChannel.SMS, capturedChannel.get());
        assertEquals("login", captured.get().key().purpose());
        assertEquals("+8613800000000", captured.get().key().subject());
        assertEquals(policy, captured.get().policy());
        assertEquals("123456", capturedCode.get());
        assertEquals(result.expiresAt(), capturedExpiresAt.get());
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
    void exposesOnlyStandardConstructor() {
        var constructors = SmsVerificationService.class.getConstructors();

        assertEquals(1, constructors.length);
        assertArrayEquals(
                new Class<?>[] {
                    CodeGenerator.class,
                    VerificationStore.class,
                    IssueRateLimiter.class,
                    VerificationPolicy.class,
                    SmsCodeSender.class
                },
                constructors[0].getParameterTypes());
    }

    @Test
    void allowsContextCustomizationWithoutChangingSmsChannel() throws Exception {
        AtomicReference<IssueContext> captured = new AtomicReference<>();
        AtomicReference<IssueContext> dispatched = new AtomicReference<>();
        CustomSmsVerificationService service = new CustomSmsVerificationService(
                (context, requestedAt) -> {
                    captured.set(context);
                    return new io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult.Allowed();
                },
                (context, code, expiresAt) -> {
                    dispatched.set(context);
                    return CodeSendResult.ACCEPTED;
                });

        service.issue(new VerificationKey("account", "login", "+8613800000000"));

        assertEquals("device-1", captured.get().attribute("device-id").orElseThrow());
        assertEquals(VerificationChannel.SMS, captured.get().channel());
        assertEquals("device-1", dispatched.get().attribute("device-id").orElseThrow());
        assertEquals(VerificationPolicy.defaults(), dispatched.get().policy());
        assertTrue(Modifier.isFinal(
                SmsVerificationService.class.getDeclaredMethod("channel").getModifiers()));
    }

    private static final class CustomSmsVerificationService extends SmsVerificationService {

        private CustomSmsVerificationService(IssueRateLimiter limiter, SmsCodeSender sender) {
            super(length -> "123456", new InMemoryVerificationStore(), limiter, VerificationPolicy.defaults(), sender);
        }

        @Override
        protected IssueContext customizeIssueContext(IssueContext context) {
            return context.with("device-id", "device-1");
        }
    }
}
