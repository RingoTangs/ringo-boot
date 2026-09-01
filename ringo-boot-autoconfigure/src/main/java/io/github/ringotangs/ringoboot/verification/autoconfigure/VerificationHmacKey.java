package io.github.ringotangs.ringoboot.verification.autoconfigure;

import java.util.Base64;
import java.util.Objects;

/**
 * 验证码能力使用的 HMAC 密钥。
 *
 * <p>密钥应由至少 32 字节的密码学安全随机数据生成，再编码为 Base64 字符串。Base64 只负责将二进制数据转换为文本，
 * 不会增加密钥的随机性或安全强度。推荐使用 OpenSSL 生成：</p>
 *
 * <pre>{@code
 * openssl rand -base64 32
 * }</pre>
 *
 * <p>也可以使用 Java 生成：</p>
 *
 * <pre>{@code
 * byte[] bytes = new byte[32];
 * new java.security.SecureRandom().nextBytes(bytes);
 * String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
 * VerificationHmacKey hmacKey = VerificationHmacKey.fromBase64(encoded);
 * }</pre>
 *
 * <p>同一应用的所有实例必须使用相同密钥。生产环境应通过环境变量或 Secret Manager 注入，不应写入源码、提交到配置
 * 仓库或输出到日志。更换密钥后，使用旧密钥生成的摘要将无法继续匹配，因此应根据密钥消费者的状态生命周期安排轮换。</p>
 */
public final class VerificationHmacKey {

    private static final int MINIMUM_KEY_BYTES = 32;

    private final byte[] encoded;

    private VerificationHmacKey(byte[] encoded) {
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
    public static VerificationHmacKey of(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded must not be null");
        if (encoded.length < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException("encoded must contain at least 32 bytes");
        }
        return new VerificationHmacKey(encoded.clone());
    }

    /**
     * 解码 Base64 字符串并创建 HMAC 密钥。
     *
     * <p>Base64 字符串可以使用 {@code openssl rand -base64 32} 生成。解码后的内容必须包含至少 32 字节的随机数据。</p>
     *
     * @param encoded Base64 编码的密钥
     * @return HMAC 密钥
     * @throws NullPointerException 当密钥为 {@code null} 时
     * @throws IllegalArgumentException 当字符串不是合法 Base64 或解码后少于 32 字节时
     */
    public static VerificationHmacKey fromBase64(String encoded) {
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
