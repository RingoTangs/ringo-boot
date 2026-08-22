package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VerificationKeyTest {

    @Test
    void createsKeyWithBusinessNamespacePurposeAndSubject() {
        VerificationKey key = new VerificationKey("user-account2", "reset-password", " User@example.com ");

        assertEquals("user-account2", key.namespace());
        assertEquals("reset-password", key.purpose());
        assertEquals(" User@example.com ", key.subject());
        assertNotEquals(key, new VerificationKey("payment", key.purpose(), key.subject()));
    }

    @Test
    void rejectsNullAndBlankValues() {
        assertThrows(NullPointerException.class, () -> new VerificationKey(null, "login", "subject"));
        assertThrows(NullPointerException.class, () -> new VerificationKey("account", null, "subject"));
        assertThrows(NullPointerException.class, () -> new VerificationKey("account", "login", null));
        assertThrows(IllegalArgumentException.class, () -> new VerificationKey("account", "login", " "));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", " ", "Account", "user_account", "user:account", "-account", "account-", "user--account"})
    void rejectsInvalidNamespace(String namespace) {
        assertThrows(IllegalArgumentException.class, () -> new VerificationKey(namespace, "login", "subject"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Login", "reset_password", "reset:password", "-login", "login-", "user--login"})
    void rejectsInvalidPurpose(String purpose) {
        assertThrows(IllegalArgumentException.class, () -> new VerificationKey("account", purpose, "subject"));
    }
}
