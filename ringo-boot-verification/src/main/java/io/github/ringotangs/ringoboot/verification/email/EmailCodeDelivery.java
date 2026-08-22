package io.github.ringotangs.ringoboot.verification.email;

import java.time.Instant;
import java.util.Objects;

/**
 * 描述一次待发送的邮件验证码。
 *
 * @param namespace 验证码所属的业务命名空间
 * @param purpose 验证码用途
 * @param email 接收验证码的邮箱地址
 * @param code 仅供发送期间使用的明文验证码
 * @param expiresAt 验证码过期时间
 */
public record EmailCodeDelivery(String namespace, String purpose, String email, String code, Instant expiresAt) {

    /**
     * 创建邮件验证码发送内容。
     *
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当任一字符串参数为空白时
     */
    public EmailCodeDelivery {
        namespace = requireText(namespace, "namespace");
        purpose = requireText(purpose, "purpose");
        email = requireText(email, "email");
        code = requireText(code, "code");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 返回隐藏邮箱地址和明文验证码的安全字符串表示。
     *
     * @return 脱敏后的字符串表示
     */
    @Override
    public String toString() {
        return "EmailCodeDelivery[namespace=" + namespace + ", purpose=" + purpose
                + ", email=<redacted>, code=<redacted>, expiresAt=" + expiresAt + "]";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
