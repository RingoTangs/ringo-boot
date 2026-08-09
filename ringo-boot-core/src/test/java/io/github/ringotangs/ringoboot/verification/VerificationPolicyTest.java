package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class VerificationPolicyTest {

    @Test
    void providesSecureDefaults() {
        VerificationPolicy policy = VerificationPolicy.defaults();

        assertEquals(6, policy.length());
        assertEquals(Duration.ofMinutes(5), policy.ttl());
        assertEquals(5, policy.maxAttempts());
        assertEquals(Duration.ofSeconds(60), policy.resendInterval());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationPolicy(0, Duration.ofMinutes(1), 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new VerificationPolicy(6, Duration.ZERO, 1, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationPolicy(6, Duration.ofMinutes(1), 0, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VerificationPolicy(6, Duration.ofMinutes(1), 1, Duration.ofSeconds(-1)));
    }
}
