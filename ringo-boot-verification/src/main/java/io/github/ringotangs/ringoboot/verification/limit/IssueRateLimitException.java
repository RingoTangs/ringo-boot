package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 验证码签发限流异常的统一抽象父类。
 *
 * <p>调用方可以捕获该类型统一处理正常超限、规则配置缺失和限流存储故障，并通过具体子类区分不同语义。
 */
public abstract class IssueRateLimitException extends VerificationException {

    /**
     * 使用诊断消息创建异常。
     *
     * @param message 诊断消息
     * @throws NullPointerException 当诊断消息为 {@code null} 时
     */
    protected IssueRateLimitException(String message) {
        super(message);
    }

    /**
     * 使用诊断消息和原始异常创建异常。
     *
     * @param message 诊断消息
     * @param cause 原始异常
     * @throws NullPointerException 当诊断消息或原始异常为 {@code null} 时
     */
    protected IssueRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
