package io.github.ringotangs.ringoboot.verification;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * 使用密码学安全随机源生成纯数字验证码。
 *
 * <p>Generates numeric verification codes with a cryptographically secure random
 * source.</p>
 */
public final class NumericCodeGenerator implements CodeGenerator {

    private static final int RADIX = 10;

    private final SecureRandom random;

    /**
     * 使用新的 {@link SecureRandom} 创建数字验证码生成器。
     *
     * <p>Creates a numeric code generator with a new {@link SecureRandom} instance.</p>
     */
    public NumericCodeGenerator() {
        this(new SecureRandom());
    }

    /**
     * 使用指定的密码学安全随机源创建数字验证码生成器。
     *
     * <p>Creates a numeric code generator with the supplied cryptographically secure
     * random source.</p>
     *
     * @param random 密码学安全随机源 / the cryptographically secure random source
     * @throws NullPointerException 当随机源为 {@code null} 时 / if the random source is {@code null}
     */
    public NumericCodeGenerator(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * 生成指定长度的纯数字验证码，保留可能出现的前导零。
     *
     * <p>Generates a numeric code of the requested length, preserving any leading
     * zeroes.</p>
     *
     * @param length 验证码长度 / the requested code length
     * @return 纯数字验证码 / the numeric verification code
     * @throws IllegalArgumentException 当长度小于或等于零时 / if the length is less than or equal to zero
     */
    @Override
    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than 0: " + length);
        }
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(random.nextInt(RADIX));
        }
        return code.toString();
    }
}
