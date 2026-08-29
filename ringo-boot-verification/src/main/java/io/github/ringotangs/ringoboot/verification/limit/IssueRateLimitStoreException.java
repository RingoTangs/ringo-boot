package io.github.ringotangs.ringoboot.verification.limit;

/**
 * 表示验证码签发限流状态存储或原子操作失败。
 *
 * <p>该异常用于包装 Redis、网络、脚本协议或密码算法等基础设施故障，不表示正常达到额度上限。诊断消息可能包含内部信息，
 * 不应直接作为客户端错误详情返回。
 */
public final class IssueRateLimitStoreException extends IssueRateLimitException {

    /**
     * 使用诊断消息创建限流存储异常。
     *
     * @param message 诊断消息
     */
    public IssueRateLimitStoreException(String message) {
        super(message);
    }

    /**
     * 使用诊断消息和原始异常创建限流存储异常。
     *
     * @param message 诊断消息
     * @param cause   原始异常
     */
    public IssueRateLimitStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
