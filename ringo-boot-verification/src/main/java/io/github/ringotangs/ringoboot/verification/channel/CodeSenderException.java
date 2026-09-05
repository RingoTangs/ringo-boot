package io.github.ringotangs.ringoboot.verification.channel;

import io.github.ringotangs.ringoboot.verification.VerificationException;
import java.util.Objects;

/**
 * 表示验证码渠道派发操作失败。
 *
 * <p>请求超时、响应丢失等无法确认供应商接受状态的情况应返回 CodeSendResult.UNKNOWN。
 * 抛出 CodeSenderException 会触发服务撤销验证码；适配器必须区分确定失败与接受状态不确定。
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
