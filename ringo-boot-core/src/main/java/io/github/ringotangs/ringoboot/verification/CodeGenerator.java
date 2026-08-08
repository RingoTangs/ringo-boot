package io.github.ringotangs.ringoboot.verification;

/**
 * 生成非空白且长度与请求完全一致的验证码。
 *
 * <p>Generates a non-blank verification code whose length exactly matches the
 * requested length.</p>
 */
@FunctionalInterface
public interface CodeGenerator {

    /**
     * 生成指定长度的验证码。
     *
     * <p>Generates a verification code of the requested length.</p>
     *
     * @param length 验证码长度 / the requested code length
     * @return 生成的验证码 / the generated verification code
     * @throws IllegalArgumentException 当长度不受实现支持时 / if the length is not supported by the
     *     implementation
     */
    String generate(int length);
}
