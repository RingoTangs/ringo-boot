package io.github.ringotangs.ringoboot.verification.generator;

import java.util.Objects;

/**
 * 表示验证码生成过程或生成结果契约失败。
 *
 * <p>Indicates a verification code generation failure or an invalid generated result.</p>
 */
public class CodeGenerationException extends RuntimeException {

    /**
     * 使用非空诊断消息创建异常。
     *
     * <p>Creates an exception with a non-null diagnostic message.</p>
     *
     * @param message 诊断消息 / the diagnostic message
     * @throws NullPointerException 当消息为 {@code null} 时 / if the message is {@code null}
     */
    public CodeGenerationException(String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }

    /**
     * 使用非空诊断消息和原始异常创建异常。
     *
     * <p>Creates an exception with a non-null diagnostic message and original cause.</p>
     *
     * @param message 诊断消息 / the diagnostic message
     * @param cause 原始异常 / the original cause
     * @throws NullPointerException 当消息或原始异常为 {@code null} 时 / if the message or cause is {@code null}
     */
    public CodeGenerationException(String message, Throwable cause) {
        super(
                Objects.requireNonNull(message, "message must not be null"),
                Objects.requireNonNull(cause, "cause must not be null"));
    }
}
