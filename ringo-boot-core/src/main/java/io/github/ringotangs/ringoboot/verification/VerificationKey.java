package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 使用用途和主体唯一标识一次验证码验证流程。
 *
 * <p>Uniquely identifies a verification flow by its purpose and subject.</p>
 *
 * @param purpose 验证用途，例如登录或注册 / the verification purpose, such as login or registration
 * @param subject 被验证的主体，例如手机号、邮箱或图片验证码 ID / the subject being verified, such as a phone
 *     number, email address, or image captcha ID
 */
public record VerificationKey(String purpose, String subject) {

    /**
     * 创建并校验验证码键。
     *
     * <p>Creates and validates a verification key.</p>
     *
     * @throws NullPointerException 当用途或主体为 {@code null} 时 / if the purpose or subject is {@code null}
     * @throws IllegalArgumentException 当用途或主体为空白时 / if the purpose or subject is blank
     */
    public VerificationKey {
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }
}
