package io.github.ringotangs.ringoboot.verification;

import java.time.Instant;
import java.util.Objects;

/**
 * 描述一次待发送的验证码交付。
 *
 * <p>Describes a verification code delivery to be sent.</p>
 *
 * @param key 验证码键及目标主体 / the verification key and target subject
 * @param code 仅供发送期间使用的明文验证码 / the plaintext code used only during delivery
 * @param expiresAt 验证码过期时间 / the code expiration instant
 */
public record CodeDelivery(VerificationKey key, String code, Instant expiresAt) {

    /**
     * 创建并校验验证码交付内容。
     *
     * <p>Creates and validates a verification code delivery.</p>
     *
     * @throws NullPointerException 当任一参数为 {@code null} 时 / if any argument is {@code null}
     */
    public CodeDelivery {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 返回隐藏明文验证码的安全字符串表示。
     *
     * <p>Returns a safe string representation with the plaintext code redacted.</p>
     *
     * @return 脱敏后的字符串表示 / a redacted string representation
     */
    @Override
    public String toString() {
        return "CodeDelivery[key=" + key + ", code=<redacted>, expiresAt=" + expiresAt + "]";
    }
}
