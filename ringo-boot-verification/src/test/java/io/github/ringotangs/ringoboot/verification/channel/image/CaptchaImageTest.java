package io.github.ringotangs.ringoboot.verification.channel.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CaptchaImageTest {

    @Test
    void protectsImageContent() {
        byte[] source = {1, 2, 3};
        CaptchaImage image = new CaptchaImage("image/png", source);

        source[0] = 9;
        byte[] returned = image.content();
        returned[1] = 9;

        assertEquals("image/png", image.mediaType());
        assertArrayEquals(new byte[] {1, 2, 3}, image.content());
        assertEquals("CaptchaImage[mediaType=image/png, size=3]", image.toString());
    }

    @Test
    void rejectsInvalidImageContent() {
        assertThrows(NullPointerException.class, () -> new CaptchaImage(null, new byte[] {1}));
        assertThrows(IllegalArgumentException.class, () -> new CaptchaImage("text/plain", new byte[] {1}));
        assertThrows(
                IllegalArgumentException.class, () -> new CaptchaImage("image/png\r\nX-Test: true", new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new CaptchaImage("image/png", null));
        assertThrows(IllegalArgumentException.class, () -> new CaptchaImage("image/png", new byte[0]));
    }
}
