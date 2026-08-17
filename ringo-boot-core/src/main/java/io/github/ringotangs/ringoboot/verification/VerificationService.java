package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;

/**
 * 签发、派发并校验短期有效、成功后一次性消费的验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 此接口仅定义业务能力；验证码生成、存储和渠道派发由
 *     {@link AbstractVerificationService} 统一编排。
 */
public interface VerificationService {

    /**
     * 使用默认策略签发并派发验证码。
     *
     *
     * @param key 验证码键
     * @return 不包含明文验证码的签发结果
     * @throws CodeGenerationException 当验证码生成失败时
     * @throws CodeSenderException 当验证码渠道派发失败时
     * @throws NullPointerException 当验证码键为 {@code null} 时
     * @throws VerificationStoreException 当验证码存储操作失败时
     */
    IssueResult issue(VerificationKey key)
            throws CodeGenerationException, CodeSenderException, VerificationStoreException;

    /**
     * 使用指定策略签发并派发验证码。
     *
     *
     * @param key 验证码键
     * @param policy 验证码策略
     * @return 不包含明文验证码的签发结果
     * @throws CodeGenerationException 当验证码生成失败时
     * @throws CodeSenderException 当验证码渠道派发失败时
     * @throws NullPointerException 当验证码键或策略为 {@code null} 时
     * @throws VerificationStoreException 当验证码存储操作失败时
     */
    IssueResult issue(VerificationKey key, VerificationPolicy policy)
            throws CodeGenerationException, CodeSenderException, VerificationStoreException;

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
