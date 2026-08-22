package io.github.ringotangs.ringoboot.verification.generator;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * 使用密码学安全随机源生成纯数字验证码。
 *
 */
public final class NumericCodeGenerator implements CodeGenerator {

    private static final int RADIX = 10;

    private final SecureRandom random;

    /**
     * 使用新的 {@link SecureRandom} 创建数字验证码生成器。
     *
     */
    public NumericCodeGenerator() {
        this(new SecureRandom());
    }

    /**
     * 使用指定的密码学安全随机源创建数字验证码生成器。
     *
     *
     * @param random 密码学安全随机源
     * @throws NullPointerException 当随机源为 {@code null} 时
     */
    public NumericCodeGenerator(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * 生成指定长度的纯数字验证码，保留可能出现的前导零。
     *
     *
     * @param length 验证码长度
     * @return 纯数字验证码
     * @throws IllegalArgumentException 当长度小于或等于零时
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
