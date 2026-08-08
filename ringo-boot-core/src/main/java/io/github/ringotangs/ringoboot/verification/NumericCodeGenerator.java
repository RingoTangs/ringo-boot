package io.github.ringotangs.ringoboot.verification;

import java.security.SecureRandom;
import java.util.Objects;

/** Generates numeric codes with a cryptographically secure random source. */
public final class NumericCodeGenerator implements CodeGenerator {

    private static final int RADIX = 10;

    private final SecureRandom random;

    public NumericCodeGenerator() {
        this(new SecureRandom());
    }

    public NumericCodeGenerator(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0: " + length);
        }
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(random.nextInt(RADIX));
        }
        return code.toString();
    }
}
