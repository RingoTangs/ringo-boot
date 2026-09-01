package io.github.ringotangs.ringoboot.verification.context;

import java.util.Objects;

/** 校验签发上下文在扩展过程中必须保持的不变量。 */
public final class IssueContextValidator {

    private IssueContextValidator() {}

    /**
     * 校验扩展后的上下文保留原有验证码键、渠道和策略。
     *
     * @param expected 扩展前的上下文
     * @param actual 扩展后的上下文
     * @param source 上下文来源，用于错误诊断
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当验证码键、渠道或策略发生变化时
     */
    public static void requirePreservedContext(IssueContext expected, IssueContext actual, String source) {
        Objects.requireNonNull(expected, "expected must not be null");
        Objects.requireNonNull(actual, "actual must not be null");
        Objects.requireNonNull(source, "source must not be null");
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
