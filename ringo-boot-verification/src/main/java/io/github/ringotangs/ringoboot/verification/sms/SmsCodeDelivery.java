package io.github.ringotangs.ringoboot.verification.sms;

import java.time.Instant;
import java.util.Objects;

/**
 * 描述一次待发送的短信验证码。
 */
public record SmsCodeDelivery(String namespace, String purpose, String phoneNumber, String code, Instant expiresAt) {

    /**
     * 创建短信验证码发送内容。
     *
     * @param namespace   验证码所属的业务命名空间
     * @param purpose     验证码用途
     * @param phoneNumber 接收验证码的手机号码
     * @param code        仅供发送期间使用的明文验证码
     * @param expiresAt   验证码过期时间
     * @throws NullPointerException     当任一参数为 {@code null} 时
     * @throws IllegalArgumentException 当任一字符串参数为空白时
     */
    public SmsCodeDelivery(String namespace, String purpose, String phoneNumber, String code, Instant expiresAt) {
        this.namespace = requireText(namespace, "namespace");
        this.purpose = requireText(purpose, "purpose");
        this.phoneNumber = requireText(phoneNumber, "phoneNumber");
        this.code = requireText(code, "code");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * @return 验证码所属的业务命名空间
     */
    @Override
    public String namespace() {
        return namespace;
    }

    /**
     * @return 验证码用途
     */
    @Override
    public String purpose() {
        return purpose;
    }

    /**
     * @return 接收验证码的手机号码
     */
    @Override
    public String phoneNumber() {
        return phoneNumber;
    }

    /**
     * @return 仅供发送期间使用的明文验证码
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * @return 验证码过期时间
     */
    @Override
    public Instant expiresAt() {
        return expiresAt;
    }

    /**
     * 返回隐藏手机号码和明文验证码的安全字符串表示。
     *
     * @return 脱敏后的字符串表示
     */
    @Override
    public String toString() {
        return "SmsCodeDelivery[namespace=" + namespace + ", purpose=" + purpose
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
