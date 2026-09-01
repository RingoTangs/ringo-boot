package io.github.ringotangs.ringoboot.verification.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VerificationChannelTest {

    @Test
    void providesBuiltInAndCustomChannels() {
        assertEquals("email", VerificationChannel.EMAIL.value());
        assertEquals("sms", VerificationChannel.SMS.value());
        assertEquals(new VerificationChannel("image-code"), VerificationChannel.of("image-code"));
        assertEquals("voice", VerificationChannel.of("voice").toString());
    }

    @Test
    void rejectsInvalidChannelValuesWithoutConvertingThem() {
        assertThrows(NullPointerException.class, () -> VerificationChannel.of(null));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of(""));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of("Email"));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of("image_code"));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of("image--code"));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of("-image"));
        assertThrows(IllegalArgumentException.class, () -> VerificationChannel.of("image-"));
    }
}
