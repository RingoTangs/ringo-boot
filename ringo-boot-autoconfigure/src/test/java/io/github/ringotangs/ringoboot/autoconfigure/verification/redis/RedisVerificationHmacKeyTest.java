package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class RedisVerificationHmacKeyTest {

    @Test
    void protectsKeyBytesWithDefensiveCopies() {
        byte[] source = new byte[32];
        source[0] = 1;

        RedisVerificationHmacKey hmacKey = RedisVerificationHmacKey.of(source);
        source[0] = 2;
        byte[] returned = hmacKey.getEncoded();
        returned[0] = 3;

        assertThat(hmacKey.getEncoded()[0]).isEqualTo((byte) 1);
    }

    @Test
    void createsKeyFromBase64() {
        byte[] source = new byte[32];
        source[31] = 1;

        RedisVerificationHmacKey hmacKey =
                RedisVerificationHmacKey.fromBase64(Base64.getEncoder().encodeToString(source));

        assertThat(hmacKey.getEncoded()).containsExactly(source);
    }

    @Test
    void rejectsNullKey() {
        assertThatNullPointerException()
                .isThrownBy(() -> RedisVerificationHmacKey.of(null))
                .withMessage("encoded must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> RedisVerificationHmacKey.fromBase64(null))
                .withMessage("encoded must not be null");
    }

    @Test
    void rejectsShortKey() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RedisVerificationHmacKey.of(new byte[31]))
                .withMessage("encoded must contain at least 32 bytes");
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        RedisVerificationHmacKey.fromBase64(Base64.getEncoder().encodeToString(new byte[31])))
                .withMessage("encoded must be valid Base64 and contain at least 32 bytes");
    }

    @Test
    void rejectsInvalidBase64() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RedisVerificationHmacKey.fromBase64("not-base64!"))
                .withMessage("encoded must be valid Base64 and contain at least 32 bytes");
    }
}
