package io.github.ringotangs.ringoboot.verification.channel.image;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示图片验证码渲染或编码失败。
 */
public final class CaptchaRenderingException extends VerificationException {

    /**
     * 使用诊断消息创建异常。
     */
    public CaptchaRenderingException(String message) {
        super(message);
    }

    /**
     * 使用诊断消息和原始异常创建异常。
     */
    public CaptchaRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
