package io.github.ringotangs.ringoboot.verification.channel.image;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ImageCaptchaResultTest {

    @Test
    void createsValidatedImageCaptchaResult() {
        Instant expiresAt = Instant.parse("2026-01-01T00:05:00Z");
        CaptchaImage image = new CaptchaImage("image/png", new byte[] {1});
        ImageCaptchaResult result = new ImageCaptchaResult(expiresAt, image);

        assertSame(expiresAt, result.expiresAt());
        assertSame(image, result.image());
        assertThrows(NullPointerException.class, () -> new ImageCaptchaResult(null, image));
        assertThrows(NullPointerException.class, () -> new ImageCaptchaResult(expiresAt, null));
    }
}
