package io.github.ringotangs.ringoboot.verification;

/**
 * 签发并校验短期有效、成功后一次性消费的验证码。
 *
 * <p>Issues and verifies short-lived codes that are consumed after successful
 * verification.</p>
 */
public interface VerificationService {

    /**
     * 使用默认策略签发验证码。
     *
     * <p>Issues a verification code with the default policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @return 签发结果 / the issuance result
     * @throws NullPointerException 当验证码键为 {@code null} 时 / if the verification key is {@code null}
     */
    IssueResult issue(VerificationKey key);

    /**
     * 使用指定策略签发验证码。
     *
     * <p>Issues a verification code with the supplied policy.</p>
     *
     * @param key 验证码键 / the verification key
     * @param policy 验证码策略 / the verification policy
     * @return 签发结果 / the issuance result
     * @throws NullPointerException 当验证码键或策略为 {@code null} 时 / if the key or policy is {@code null}
     */
    IssueResult issue(VerificationKey key, VerificationPolicy policy);

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
     */
    VerificationResult verify(VerificationKey key, String code);

    /**
     * 当验证码键与明文验证码同时匹配时原子地使其失效。
     *
     * <p>Atomically invalidates a code only when both its key and plaintext value
     * match.</p>
     *
     * @param key 验证码键 / the verification key
     * @param code 待失效的明文验证码 / the plaintext code to invalidate
     * @return 是否删除了匹配记录 / whether a matching record was removed
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时 / if the key or code is {@code null}
     */
    boolean invalidate(VerificationKey key, String code);
}
