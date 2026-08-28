package io.github.ringotangs.ringoboot.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KebabCaseTest {

    @Test
    void acceptsLowercaseNamesWithDigitsAndSingleHyphens() {
        assertDoesNotThrow(() -> KebabCase.validate("value", "account"));
        assertDoesNotThrow(() -> KebabCase.validate("value", "account2"));
        assertDoesNotThrow(() -> KebabCase.validate("value", "user-account2"));
        assertDoesNotThrow(() -> KebabCase.validate("value", "reset-password"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> KebabCase.validate(null, "account"));
        assertThrows(NullPointerException.class, () -> KebabCase.validate("value", null));
    }

    @Test
    void rejectsInvalidNamesWithoutConvertingThem() {
        assertInvalid("");
        assertInvalid(" ");
        assertInvalid("User");
        assertInvalid("user_account");
        assertInvalid("user account");
        assertInvalid("user:account");
        assertInvalid("-user");
        assertInvalid("user-");
        assertInvalid("user--account");
    }

    private static void assertInvalid(String value) {
        assertThrows(IllegalArgumentException.class, () -> KebabCase.validate("value", value));
    }
}
