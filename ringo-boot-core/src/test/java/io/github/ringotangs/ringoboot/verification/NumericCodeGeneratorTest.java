package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class NumericCodeGeneratorTest {

    @Test
    void generatesNumericCodeOfRequestedLength() {
        String code = new NumericCodeGenerator().generate(12);

        assertEquals(12, code.length());
        assertTrue(code.matches("[0-9]{12}"));
    }

    @Test
    void usesInjectedSecureRandom() {
        SecureRandom random = new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };

        assertEquals("9999", new NumericCodeGenerator(random).generate(4));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new NumericCodeGenerator().generate(0));
    }
}
