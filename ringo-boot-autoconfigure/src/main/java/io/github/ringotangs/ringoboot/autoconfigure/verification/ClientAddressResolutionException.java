package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;

/** 表示客户端来源地址无法用于验证码签发限流。 */
public final class ClientAddressResolutionException extends IssueRateLimitException {

    /** 使用诊断消息创建异常。 */
    public ClientAddressResolutionException(String message) {
        super(message);
    }

    /** 使用诊断消息和原始异常创建异常。 */
    public ClientAddressResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
