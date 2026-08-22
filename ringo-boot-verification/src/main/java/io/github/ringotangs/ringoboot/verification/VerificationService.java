package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;

/**
 * 签发、派发并校验短期有效、成功后一次性消费的验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 此接口仅定义业务能力；验证码生成、存储、渠道派发和签发限流由
 * {@link AbstractVerificationService} 统一编排。IP、设备等环境限流信号由限流组件集中解析，业务调用方只需传递验证码键。
 */
public interface VerificationService {

    /**
     * 使用默认策略签发并派发验证码。
     *
     *
     * @param key 验证码键
     * @return 不包含明文验证码的签发结果
     * @throws VerificationException 当验证码生成、签发限流、存储或渠道派发失败时
     * @throws NullPointerException 当验证码键为 {@code null} 时
     */
    IssueResult issue(VerificationKey key) throws VerificationException;

    /**
     * 使用指定策略签发并派发验证码。
     *
     *
     * @param key 验证码键
     * @param policy 验证码策略
     * @return 不包含明文验证码的签发结果
     * @throws VerificationException 当验证码生成、签发限流、存储或渠道派发失败时
     * @throws NullPointerException 当验证码键或策略为 {@code null} 时
     */
    IssueResult issue(VerificationKey key, VerificationPolicy policy) throws VerificationException;

    /**
     * 校验验证码，并根据结果原子地消费记录或扣减剩余尝试次数。
     *
     *
     * @param key 验证码键
     * @param code 待校验的验证码
     * @return 校验结果
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时
     * @throws VerificationStoreException 当验证码存储操作失败时
     */
    VerificationResult verify(VerificationKey key, String code) throws VerificationStoreException;
}
