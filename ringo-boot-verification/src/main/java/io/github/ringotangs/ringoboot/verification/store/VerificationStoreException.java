package io.github.ringotangs.ringoboot.verification.store;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示验证码存储基础设施操作失败。
 *
 */
public class VerificationStoreException extends VerificationException {

    /**
     * 使用非空诊断消息创建异常。
     *
     *
     * @param message 诊断消息
     * @throws NullPointerException 当消息为 {@code null} 时
     */
    public VerificationStoreException(String message) {
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
    public VerificationStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
