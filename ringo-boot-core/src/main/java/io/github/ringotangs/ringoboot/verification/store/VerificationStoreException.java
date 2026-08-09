package io.github.ringotangs.ringoboot.verification.store;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示验证码存储基础设施操作失败。
 *
 * <p>Indicates a verification storage infrastructure operation failure.</p>
 */
public class VerificationStoreException extends VerificationException {

    /**
     * 使用非空诊断消息创建异常。
     *
     * <p>Creates an exception with a non-null diagnostic message.</p>
     *
     * @param message 诊断消息 / the diagnostic message
     * @throws NullPointerException 当消息为 {@code null} 时 / if the message is {@code null}
     */
    public VerificationStoreException(String message) {
        super(message);
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
    public VerificationStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
