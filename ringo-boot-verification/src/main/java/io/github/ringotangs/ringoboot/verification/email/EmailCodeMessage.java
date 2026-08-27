package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
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
public record EmailCodeMessage(String namespace, String purpose, String email, String code, Instant expiresAt) {

    /**
     * 创建邮件验证码发送内容。
     *
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当任一字符串参数为空白时
     */
    public EmailCodeMessage {
        namespace = requireText(namespace, "namespace");
        purpose = requireText(purpose, "purpose");
        email = requireText(email, "email");
        code = requireText(code, "code");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 根据邮件签发上下文创建待发送消息。
     *
     * @param context 邮件签发上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 不包含上下文扩展属性的邮件消息
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当上下文渠道不是邮件时
     */
    static EmailCodeMessage from(IssueContext context, String code, Instant expiresAt) {
        Objects.requireNonNull(context, "context must not be null");
        if (!VerificationChannel.EMAIL.equals(context.channel())) {
            throw new IllegalArgumentException("issue context channel must be EMAIL");
        }
        VerificationKey key = context.key();
        return new EmailCodeMessage(key.namespace(), key.purpose(), key.subject(), code, expiresAt);
    }

    /**
     * 返回隐藏邮箱地址和明文验证码的安全字符串表示。
     *
     * @return 脱敏后的字符串表示
     */
    @Override
    public String toString() {
        return "EmailCodeMessage[namespace=" + namespace + ", purpose=" + purpose
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
