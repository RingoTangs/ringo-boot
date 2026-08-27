package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 表示验证码签发和交付使用的稳定渠道。
 *
 * <p>框架内置邮件和短信渠道，应用也可以使用 {@link #of(String)} 创建图片、语音等自定义渠道。
 * 渠道名称不会自动转换，避免不同应用对同一渠道使用不一致的稳定标识。
 *
 * @param value 小写 kebab-case 渠道标识
 */
public record VerificationChannel(String value) {

    private static final Pattern VALUE_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /**
     * 邮件验证码渠道。
     */
    public static final VerificationChannel EMAIL = new VerificationChannel("email");

    /**
     * 短信验证码渠道。
     */
    public static final VerificationChannel SMS = new VerificationChannel("sms");

    /**
     * 创建并校验验证码渠道。
     *
     * @throws NullPointerException     当渠道标识为 {@code null} 时
     * @throws IllegalArgumentException 当渠道标识不是小写 kebab-case 时
     */
    public VerificationChannel {
        Objects.requireNonNull(value, "value must not be null");
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be lowercase kebab-case: " + value);
        }
    }

    /**
     * 创建自定义验证码渠道。
     *
     * @param value 小写 kebab-case 渠道标识
     * @return 验证码渠道
     */
    public static VerificationChannel of(String value) {
        return new VerificationChannel(value);
    }

    /**
     * 返回渠道的稳定字符串标识。
     *
     * @return 渠道标识
     */
    @Override
    public String toString() {
        return value;
    }
}
