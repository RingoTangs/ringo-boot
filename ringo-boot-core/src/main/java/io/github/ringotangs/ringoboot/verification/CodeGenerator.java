package io.github.ringotangs.ringoboot.verification;

/** Generates a non-blank verification code of exactly the requested length. */
@FunctionalInterface
public interface CodeGenerator {

    String generate(int length);
}
