package io.github.ringotangs.ringoboot.verification;

import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;

/**
 * 签发、派发并校验短期有效、成功后一次性消费的验证码。
 *
 * <p>Issues, dispatches, and verifies short-lived codes that are consumed after
 * successful verification.</p>
 *
 * @apiNote 此接口仅定义业务能力；验证码生成、存储和渠道派发由
 *     {@link AbstractVerificationService} 统一编排。 / This interface defines business capabilities
 *     only; code generation, storage, and channel dispatch are coordinated by
 *     {@link AbstractVerificationService}.
 */
public interface VerificationService {

    /**
     * 使用默认策略签发并派发验证码。
     *
     * <p>Issues and dispatches a verification code with the default policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @return 不包含明文验证码的交付结果 / the delivery result without the plaintext code
     * @throws CodeSenderException 当验证码渠道派发失败时 / if verification code delivery fails
     * @throws NullPointerException 当验证码键为 {@code null} 时 / if the verification key is {@code null}
     * @throws VerificationStoreException 当验证码存储操作失败时 / if a verification storage operation fails
     */
    DeliveryResult issue(VerificationKey key) throws CodeSenderException, VerificationStoreException;

    /**
     * 使用指定策略签发并派发验证码。
     *
     * <p>Issues and dispatches a verification code with the supplied policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @param policy 验证码策略 / the verification policy
     * @return 不包含明文验证码的交付结果 / the delivery result without the plaintext code
     * @throws CodeSenderException 当验证码渠道派发失败时 / if verification code delivery fails
     * @throws NullPointerException 当验证码键或策略为 {@code null} 时 / if the key or policy is {@code null}
     * @throws VerificationStoreException 当验证码存储操作失败时 / if a verification storage operation fails
     */
    DeliveryResult issue(VerificationKey key, VerificationPolicy policy)
            throws CodeSenderException, VerificationStoreException;

    /**
     * 校验验证码，并根据结果原子地消费记录或扣减剩余尝试次数。
     *
     * <p>Verifies a code and atomically consumes the record or decrements its remaining
     * attempts according to the outcome.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待校验的验证码 / the code to verify
     * @return 校验结果 / the verification result
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时 / if the key or code is {@code null}
     * @throws VerificationStoreException 当验证码存储操作失败时 / if a verification storage operation fails
     */
    VerificationResult verify(VerificationKey key, String code) throws VerificationStoreException;
}
