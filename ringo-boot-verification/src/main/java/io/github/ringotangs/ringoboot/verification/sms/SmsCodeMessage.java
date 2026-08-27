package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import java.time.Instant;
import java.util.Objects;

/**
 * 描述一次待发送的短信验证码。
 *
 * @param namespace 验证码所属的业务命名空间
 * @param purpose 验证码用途
 * @param phoneNumber 接收验证码的手机号码
 * @param code 仅供发送期间使用的明文验证码
 * @param expiresAt 验证码过期时间
 */
public record SmsCodeMessage(String namespace, String purpose, String phoneNumber, String code, Instant expiresAt) {

    /**
     * 创建短信验证码发送内容。
     *
     * @throws NullPointerException     当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当任一字符串参数为空白时
     */
    public SmsCodeMessage {
        namespace = requireText(namespace, "namespace");
        purpose = requireText(purpose, "purpose");
        phoneNumber = requireText(phoneNumber, "phoneNumber");
        code = requireText(code, "code");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 根据短信签发上下文创建待发送消息。
     *
     * @param context 短信签发上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 不包含上下文扩展属性的短信消息
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当上下文渠道不是短信时
     */
    static SmsCodeMessage from(IssueContext context, String code, Instant expiresAt) {
        Objects.requireNonNull(context, "context must not be null");
        if (!VerificationChannel.SMS.equals(context.channel())) {
            throw new IllegalArgumentException("issue context channel must be SMS");
        }
        VerificationKey key = context.key();
        return new SmsCodeMessage(key.namespace(), key.purpose(), key.subject(), code, expiresAt);
    }

    /**
     * 返回隐藏手机号码和明文验证码的安全字符串表示。
     *
     * @return 脱敏后的字符串表示
     */
    @Override
    public String toString() {
        return "SmsCodeMessage[namespace=" + namespace + ", purpose=" + purpose
                + ", phoneNumber=<redacted>, code=<redacted>, expiresAt=" + expiresAt + "]";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
