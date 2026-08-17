package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 使用业务命名空间、用途和主体唯一标识一次验证码验证流程。
 *
 *
 * @param namespace 稳定的业务命名空间，例如账户或支付
 * @param purpose 验证用途，例如登录或注册
 * @param subject 被验证的主体，例如手机号、邮箱或图片验证码 ID
 */
public record VerificationKey(String namespace, String purpose, String subject) {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /**
     * 创建并校验验证码键。
     *
     *
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当命名空间或用途不是小写 kebab-case，或者主体为空白时
     */
    public VerificationKey {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        validateSegment("namespace", namespace);
        validateSegment("purpose", purpose);
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }

    private static void validateSegment(String name, String value) {
        if (!SEGMENT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase kebab-case: " + value);
        }
    }
}
