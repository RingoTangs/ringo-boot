package io.github.ringotangs.ringoboot.verification;

/** 校验签发上下文在扩展过程中必须保持的不变量。 */
final class IssueContextValidator {

    private IssueContextValidator() {}

    static void requirePreservedContext(IssueContext expected, IssueContext actual, String source) {
        if (!actual.key().equals(expected.key())) {
            throw new IllegalArgumentException(source + " must preserve the verification key");
        }
        if (!actual.channel().equals(expected.channel())) {
            throw new IllegalArgumentException(source + " must preserve the verification channel");
        }
        if (!actual.policy().equals(expected.policy())) {
            throw new IllegalArgumentException(source + " must preserve the verification policy");
        }
    }
}
