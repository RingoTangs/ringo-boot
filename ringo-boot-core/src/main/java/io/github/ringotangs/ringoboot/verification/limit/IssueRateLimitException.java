package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/**
 * 表示验证码签发限流基础设施操作失败。
 *
 * <p>该异常用于包装 Redis、网络或其他限流状态存储故障，不表示正常的配额超限。正常超限应返回
 * {@link IssueLimitResult.Throttled}。诊断消息可能包含内部基础设施信息，不应直接作为客户端错误详情返回。
 */
public class IssueRateLimitException extends VerificationException {

    /**
     * 使用诊断消息创建异常。
     *
     * @param message 诊断消息
     * @throws NullPointerException 当诊断消息为 {@code null} 时
     */
    public IssueRateLimitException(String message) {
        super(message);
    }

    /**
     * 使用诊断消息和原始异常创建异常。
     *
     * @param message 诊断消息
     * @param cause 原始异常
     * @throws NullPointerException 当诊断消息或原始异常为 {@code null} 时
     */
    public IssueRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
