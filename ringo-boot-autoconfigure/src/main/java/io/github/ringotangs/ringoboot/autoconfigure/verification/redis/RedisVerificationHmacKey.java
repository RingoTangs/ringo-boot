package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import java.util.Base64;
import java.util.Objects;

/**
 * Redis 验证码能力使用的 HMAC 密钥。
 *
 * <p>同一环境中的所有应用实例必须使用相同密钥。密钥应由环境变量或 Secret Manager 提供，不应写入源码或日志。</p>
 */
public final class RedisVerificationHmacKey {

    private static final int MINIMUM_KEY_BYTES = 32;

    private final byte[] encoded;

    private RedisVerificationHmacKey(byte[] encoded) {
        this.encoded = encoded;
    }

    /**
     * 使用二进制密钥创建 HMAC 密钥。
     *
     * @param encoded 至少 32 字节的密钥
     * @return HMAC 密钥
     * @throws NullPointerException 当密钥为 {@code null} 时
     * @throws IllegalArgumentException 当密钥少于 32 字节时
     */
    public static RedisVerificationHmacKey of(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded must not be null");
        if (encoded.length < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException("encoded must contain at least 32 bytes");
        }
        return new RedisVerificationHmacKey(encoded.clone());
    }

    /**
     * 解码 Base64 字符串并创建 HMAC 密钥。
     *
     * @param encoded Base64 编码的密钥
     * @return HMAC 密钥
     * @throws NullPointerException 当密钥为 {@code null} 时
     * @throws IllegalArgumentException 当字符串不是合法 Base64 或解码后少于 32 字节时
     */
    public static RedisVerificationHmacKey fromBase64(String encoded) {
        Objects.requireNonNull(encoded, "encoded must not be null");
        try {
            return of(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("encoded must be valid Base64 and contain at least 32 bytes", exception);
        }
    }

    /**
     * 返回密钥的防御性副本。
     *
     * @return 密钥字节副本
     */
    public byte[] getEncoded() {
        return encoded.clone();
    }
}
