package io.github.ringotangs.ringoboot.verification.channel.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Java2dImageCaptchaRendererTest {

    private static final IssueContext CONTEXT = IssueContext.of(
            new VerificationKey("security", "login-captcha", "550e8400-e29b-41d4-a716-446655440000"),
            VerificationChannel.IMAGE,
            new VerificationPolicy(4, java.time.Duration.ofMinutes(2), 3));

    @Test
    void rendersDecodablePngInHeadlessEnvironments() throws Exception {
        CaptchaImage captcha = new Java2dImageCaptchaRenderer().render(CONTEXT, "1234");
        var image = ImageIO.read(new ByteArrayInputStream(captcha.content()));

        assertEquals("image/png", captcha.mediaType());
        assertNotNull(image);
        assertEquals(160, image.getWidth());
        assertEquals(60, image.getHeight());
    }

    @Test
    void rejectsInvalidInputs() {
        Java2dImageCaptchaRenderer renderer = new Java2dImageCaptchaRenderer();

        assertThrows(NullPointerException.class, () -> new Java2dImageCaptchaRenderer((SecureRandom) null));
        assertThrows(NullPointerException.class, () -> renderer.render(null, "1234"));
        assertThrows(NullPointerException.class, () -> renderer.render(CONTEXT, null));
        assertThrows(IllegalArgumentException.class, () -> renderer.render(CONTEXT, " "));
        assertThrows(IllegalArgumentException.class, () -> renderer.render(CONTEXT, "123456789"));
    }
}
