package io.github.ringotangs.ringoboot.verification.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import org.junit.jupiter.api.Test;

class VerificationStoreKeyTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");

    @Test
    void validatesComponentsAndProvidesSafeDiagnostics() {
        VerificationStoreKey key = new VerificationStoreKey(KEY, VerificationChannel.EMAIL);

        assertEquals(KEY, key.key());
        assertEquals(VerificationChannel.EMAIL, key.channel());
        assertFalse(key.toString().contains(KEY.subject()));
        assertThrows(NullPointerException.class, () -> new VerificationStoreKey(null, VerificationChannel.EMAIL));
        assertThrows(NullPointerException.class, () -> new VerificationStoreKey(KEY, null));
    }
}
