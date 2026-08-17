package io.github.ringotangs.ringoboot.verification.generator;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示验证码生成过程或生成结果契约失败。
 *
 */
public class CodeGenerationException extends VerificationException {

    /**
     * 使用非空诊断消息创建异常。
     *
     *
     * @param message 诊断消息
     * @throws NullPointerException 当消息为 {@code null} 时
     */
    public CodeGenerationException(String message) {
        super(message);
    }

    /**
     * 使用非空诊断消息和原始异常创建异常。
     *
     *
     * @param message 诊断消息
     * @param cause 原始异常
     * @throws NullPointerException 当消息或原始异常为 {@code null} 时
     */
    public CodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
