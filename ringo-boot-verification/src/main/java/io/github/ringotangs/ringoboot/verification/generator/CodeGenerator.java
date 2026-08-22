package io.github.ringotangs.ringoboot.verification.generator;

/**
 * 生成非空白且长度与请求完全一致的验证码。
 *
 *
 * <p><strong>实现要求：</strong> 第三方适配器必须将随机源、远程服务和生成算法故障包装为
 *     {@link CodeGenerationException}，不得向调用方泄露供应商异常。
 */
@FunctionalInterface
public interface CodeGenerator {

    /**
     * 生成指定长度的验证码。
     *
     *
     * @param length 验证码长度
     * @return 生成的验证码
     * @throws CodeGenerationException 当验证码生成失败时
     * @throws IllegalArgumentException 当长度不受实现支持时
     */
    String generate(int length) throws CodeGenerationException;
}
