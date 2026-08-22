package io.github.ringotangs.ringoboot.verification.sender;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示验证码渠道派发操作失败。
 *
 */
public class CodeSenderException extends VerificationException {

    /**
     * 使用非空诊断消息创建异常。
     *
     *
     * @param message 诊断消息
     * @throws NullPointerException 当消息为 {@code null} 时
     */
    public CodeSenderException(String message) {
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
    public CodeSenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
