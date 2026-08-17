package io.github.ringotangs.ringoboot.verification.generator;

/**
 * 生成非空白且长度与请求完全一致的验证码。
 *
 * <p>Generates a non-blank verification code whose length exactly matches the
 * requested length.</p>
 *
 * <p><strong>实现要求 / Implementation requirements:</strong> 第三方适配器必须将随机源、远程服务和生成算法故障包装为
 *     {@link CodeGenerationException}，不得向调用方泄露供应商异常。 / Third-party adapters must wrap
 *     random-source, remote-service, and generation-algorithm failures in
 *     {@link CodeGenerationException} instead of exposing vendor exceptions.
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
     * @throws CodeGenerationException 当验证码生成失败时 / if verification code generation fails
     * @throws IllegalArgumentException 当长度不受实现支持时 / if the length is not supported by the
     *     implementation
     */
    String generate(int length) throws CodeGenerationException;
}
