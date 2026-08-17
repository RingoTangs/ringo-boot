package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 验证码生成、派发或存储过程中技术故障的统一抽象父类。
 *
 * <p>Base exception for technical failures during verification code generation, delivery, or
 * storage.</p>
 *
 * <p><strong>API 注意事项 / API note:</strong> 调用方可以捕获此类型统一记录或转换验证码技术异常，同时仍可捕获具体子类以区分失败来源。 /
 *     Callers may catch this type to uniformly record or translate verification failures while
 *     retaining the option to catch concrete subclasses for a specific failure source.
 */
public abstract class VerificationException extends RuntimeException {

    /**
     * 使用非空诊断消息创建验证码异常。
     *
     * <p>Creates a verification exception with a non-null diagnostic message.</p>
     *
     * @param message 诊断消息 / the diagnostic message
     * @throws NullPointerException 当消息为 {@code null} 时 / if the message is {@code null}
     */
    protected VerificationException(String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }

    /**
     * 使用非空诊断消息和原始异常创建验证码异常。
     *
     * <p>Creates a verification exception with a non-null diagnostic message and original cause.</p>
     *
     * @param message 诊断消息 / the diagnostic message
     * @param cause 原始异常 / the original cause
     * @throws NullPointerException 当消息或原始异常为 {@code null} 时 / if the message or cause is
     *     {@code null}
     */
    protected VerificationException(String message, Throwable cause) {
        super(
                Objects.requireNonNull(message, "message must not be null"),
                Objects.requireNonNull(cause, "cause must not be null"));
    }
}
