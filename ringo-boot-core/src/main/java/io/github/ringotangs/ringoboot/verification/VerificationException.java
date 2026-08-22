package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 验证码签发、校验及相关基础设施操作异常的统一抽象父类。
 *
 *
 * <p><strong>API 注意事项：</strong> 调用方可以捕获此类型统一记录或转换验证码操作异常，同时仍可捕获具体子类以区分业务拒绝、
 * 配置错误和基础设施故障。
 */
public abstract class VerificationException extends RuntimeException {

    /**
     * 使用非空诊断消息创建验证码异常。
     *
     *
     * @param message 诊断消息
     * @throws NullPointerException 当消息为 {@code null} 时
     */
    protected VerificationException(String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
    }

    /**
     * 使用非空诊断消息和原始异常创建验证码异常。
     *
     *
     * @param message 诊断消息
     * @param cause 原始异常
     * @throws NullPointerException 当消息或原始异常为 {@code null} 时
     */
    protected VerificationException(String message, Throwable cause) {
        super(
                Objects.requireNonNull(message, "message must not be null"),
                Objects.requireNonNull(cause, "cause must not be null"));
    }
}
