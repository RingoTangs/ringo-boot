package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationException;

/** 表示验证码签发限流基础设施操作失败。 */
public class IssueRateLimitException extends VerificationException {

    /**
     * 使用诊断消息创建异常。
     *
     * @param message 诊断消息
     */
    public IssueRateLimitException(String message) {
        super(message);
    }

    /**
     * 使用诊断消息和原始异常创建异常。
     *
     * @param message 诊断消息
     * @param cause 原始异常
     */
    public IssueRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
