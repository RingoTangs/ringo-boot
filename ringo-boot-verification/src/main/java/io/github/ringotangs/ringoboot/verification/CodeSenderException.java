package io.github.ringotangs.ringoboot.verification;

import java.util.Objects;

/**
 * 表示验证码渠道派发操作失败。
 */
public class CodeSenderException extends VerificationException {

    private final VerificationChannel channel;

    /**
     * 使用渠道和非空诊断消息创建异常。
     *
     * @param channel 派发渠道
     * @param message 诊断消息
     * @throws NullPointerException 当渠道或消息为 {@code null} 时
     */
    public CodeSenderException(VerificationChannel channel, String message) {
        super(message);
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    /**
     * 使用渠道、非空诊断消息和原始异常创建异常。
     *
     * @param channel 派发渠道
     * @param message 诊断消息
     * @param cause   原始异常
     * @throws NullPointerException 当渠道、消息或原始异常为 {@code null} 时
     */
    public CodeSenderException(VerificationChannel channel, String message, Throwable cause) {
        super(message, cause);
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    /**
     * 返回派发失败的验证码渠道。
     *
     * @return 派发渠道
     */
    public final VerificationChannel channel() {
        return channel;
    }
}
