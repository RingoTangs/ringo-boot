package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;

/**
 * 签发、派发并校验短期有效、成功后一次性消费的验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 此接口仅定义业务能力；验证码生成、存储、渠道派发和签发限流由
 * {@link AbstractVerificationService} 统一编排。签发服务负责创建和解析 {@link IssueContext}，业务调用方只需传递验证码键。
 *
 * @param <R> 渠道成功签发后的结果类型
 */
public interface VerificationService<R> {

    /**
     * 使用服务级验证码策略签发并派发验证码。
     *
     *
     * @param key 验证码键
     * @return 不包含明文验证码的签发结果
     * @throws IssueLimitExceededException 当签发请求达到限流额度时
     * @throws VerificationException 当验证码生成、签发限流、存储或渠道派发失败时
     * @throws NullPointerException 当验证码键为 {@code null} 时
     */
    R issue(VerificationKey key) throws VerificationException;

    /**
     * 校验验证码，并根据结果原子地消费记录或扣减剩余尝试次数。
     *
     *
     * @param key 验证码键
     * @param code 待校验的验证码
     * @throws VerificationRejectedException 当验证码校验未通过时
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时
     * @throws VerificationException 当验证码校验或存储操作失败时
     */
    void verify(VerificationKey key, String code) throws VerificationException;
}
