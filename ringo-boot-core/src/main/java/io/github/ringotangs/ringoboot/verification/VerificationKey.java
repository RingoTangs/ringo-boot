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

    /**
     * 用于校验业务命名空间和验证用途的小写 kebab-case 正则表达式。
     *
     * <p>{@code [a-z0-9]+} 要求第一个分段至少包含一个小写字母或数字；
     * {@code (?:-[a-z0-9]+)*} 允许使用单个连字符连接后续分段。因此不允许大写字母、下划线、
     * 首尾连字符或连续连字符。
     *
     * <p>合法示例：{@code account}、{@code user-account2}、{@code reset-password}。
     * 非法示例：{@code User}、{@code user_account}、{@code -user}、{@code user-}、
     * {@code user--account}。
     */
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
