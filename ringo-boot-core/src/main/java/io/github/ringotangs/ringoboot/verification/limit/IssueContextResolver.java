package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationKey;

/**
 * 根据验证码键解析一次签发限流所需的上下文。
 *
 * <p>实现可以从当前执行环境采集 IP、设备、账号或租户等限流信号，但不应执行额度检查或访问限流状态存储。Web 应用应根据自身的
 * 可信代理和设备身份策略解析请求数据，不应默认信任客户端可伪造的请求头。
 */
@FunctionalInterface
public interface IssueContextResolver {

    /**
     * 解析指定验证码键对应的签发上下文。
     *
     * <p>返回的上下文必须非空，并且必须保留传入的验证码键。
     *
     * @param key 验证码键
     * @return 包含验证码键和额外限流信号的上下文
     * @throws NullPointerException 当验证码键为 {@code null} 时
     * @throws RuntimeException 当当前执行环境无法提供实现所需的限流信号时
     */
    IssueContext resolve(VerificationKey key);
}
