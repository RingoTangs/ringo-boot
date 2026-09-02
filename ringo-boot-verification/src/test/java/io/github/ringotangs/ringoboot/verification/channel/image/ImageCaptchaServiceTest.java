package io.github.ringotangs.ringoboot.verification.channel.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.IssueResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.CompositeIssueContextManager;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ImageCaptchaServiceTest {

    private static final VerificationKey KEY =
            new VerificationKey("security", "login-captcha", "550e8400-e29b-41d4-a716-446655440000");
    private static final VerificationPolicy POLICY = new VerificationPolicy(4, Duration.ofMinutes(2), 3);

    @Test
    void issuesRendersAndConsumesImageCaptcha() {
        AtomicReference<IssueContext> renderedContext = new AtomicReference<>();
        AtomicReference<String> renderedCode = new AtomicReference<>();
        CaptchaImage image = new CaptchaImage("image/png", new byte[] {1, 2, 3});
        ImageCaptchaService service = service((context, code) -> {
            renderedContext.set(context);
            renderedCode.set(code);
            return image;
        });
        Instant earliestExpiration = Instant.now().plus(POLICY.ttl());

        IssueResult.ImageCaptcha result = assertInstanceOf(IssueResult.ImageCaptcha.class, service.issue(KEY));

        assertSame(image, result.image());
        assertEquals("1234", renderedCode.get());
        assertEquals(KEY, renderedContext.get().key());
        assertEquals(VerificationChannel.IMAGE, renderedContext.get().channel());
        assertEquals(POLICY, renderedContext.get().policy());
        org.junit.jupiter.api.Assertions.assertFalse(result.expiresAt().isBefore(earliestExpiration));
        assertEquals(VerifyResult.SUCCESS, service.verify(KEY, "1234"));
        assertEquals(VerifyResult.NOT_FOUND, service.verify(KEY, "1234"));
    }

    @Test
    void doesNotRenderWhenIssuanceIsThrottled() {
        AtomicInteger renders = new AtomicInteger();
        IssueLimiter limiter = (context, requestedAt) -> new IssueLimitResult.Throttled(
                List.of(new IssueLimitViolation("image-ip-minute", Duration.ofSeconds(30))));
        ImageCaptchaService service = service(limiter, (context, code) -> {
            renders.incrementAndGet();
            return new CaptchaImage("image/png", new byte[] {1});
        });

        IssueResult.Throttled result = assertInstanceOf(IssueResult.Throttled.class, service.issue(KEY));

        assertEquals(Duration.ofSeconds(30), result.retryAfter());
        assertEquals(0, renders.get());
    }

    @Test
    void invalidatesStoredCodeWhenRenderingFails() {
        CaptchaRenderingException failure = new CaptchaRenderingException("render failed");
        ImageCaptchaService service = service((context, code) -> {
            throw failure;
        });

        assertSame(failure, assertThrows(CaptchaRenderingException.class, () -> service.issue(KEY)));
        assertEquals(VerifyResult.NOT_FOUND, service.verify(KEY, "1234"));
    }

    @Test
    void rejectsNullRenderer() {
        assertThrows(
                NullPointerException.class,
                () -> new ImageCaptchaService(
                        length -> "1234",
                        new InMemoryVerificationStore(),
                        IssueLimiter.permitAll(),
                        POLICY,
                        new CompositeIssueContextManager(List.of()),
                        null));
    }

    private ImageCaptchaService service(ImageCaptchaRenderer renderer) {
        return service(IssueLimiter.permitAll(), renderer);
    }

    private ImageCaptchaService service(IssueLimiter limiter, ImageCaptchaRenderer renderer) {
        return new ImageCaptchaService(
                length -> "1234",
                new InMemoryVerificationStore(),
                limiter,
                POLICY,
                new CompositeIssueContextManager(List.of()),
                renderer);
    }
}
