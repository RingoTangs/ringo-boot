package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.channel.image.CaptchaImage;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitViolation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueResultTest {

    @Test
    void createsSafeIssueResults() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");

        IssueResult.Accepted accepted = new IssueResult.Accepted(expiresAt);
        IssueResult.Uncertain uncertain = new IssueResult.Uncertain(expiresAt);
        IssueResult.ImageCaptcha image =
                new IssueResult.ImageCaptcha(expiresAt, new CaptchaImage("image/png", new byte[] {1}));
        IssueResult.Throttled throttled = new IssueResult.Throttled(List.of(
                new IssueLimitViolation("subject-minute", Duration.ofSeconds(10)),
                new IssueLimitViolation("ip-hour", Duration.ofSeconds(30))));

        assertEquals(expiresAt, accepted.expiresAt());
        assertEquals(expiresAt, uncertain.expiresAt());
        assertEquals(expiresAt, image.expiresAt());
        assertEquals("image/png", image.image().mediaType());
        assertEquals(Duration.ofSeconds(30), throttled.retryAfter());
        assertEquals(
                List.of("subject-minute", "ip-hour"),
                throttled.violations().stream().map(IssueLimitViolation::ruleId).toList());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new IssueResult.Accepted(null));
        assertThrows(NullPointerException.class, () -> new IssueResult.Uncertain(null));
        assertThrows(
                NullPointerException.class,
                () -> new IssueResult.ImageCaptcha(null, new CaptchaImage("image/png", new byte[] {1})));
        assertThrows(NullPointerException.class, () -> new IssueResult.ImageCaptcha(Instant.now(), null));
        assertThrows(NullPointerException.class, () -> new IssueResult.Throttled(null));
        assertThrows(IllegalArgumentException.class, () -> new IssueResult.Throttled(List.of()));
        IssueLimitViolation duplicate = new IssueLimitViolation("subject-minute", Duration.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new IssueResult.Throttled(List.of(duplicate, duplicate)));
    }
}
