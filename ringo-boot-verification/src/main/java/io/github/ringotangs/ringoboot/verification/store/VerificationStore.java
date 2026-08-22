package io.github.ringotangs.ringoboot.verification.store;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerifyResult;
import java.time.Instant;

/**
 * 定义验证码状态的原子存储契约。实现不得持久化明文验证码。
 * 同一验证码键的写入、比对、尝试次数扣减和消费必须具备原子性。
 *
 *
 * <p><strong>实现注意事项：</strong> 分布式实现必须保证跨进程的原子操作语义
 * <p><strong>实现要求：</strong> 第三方适配器必须将连接、超时、序列化和原子操作故障包装为
 *     {@link VerificationStoreException}，不得向调用方泄露供应商异常。
 */
public interface VerificationStore {

    /**
     * 保存新的验证码状态，并覆盖同一验证码键的旧状态。
     * 实现可以在本次调用期间读取明文验证码，但只能保存不可逆的安全表示。
     *
     *
     * @param key 验证码键
     * @param code 新签发的明文验证码
     * @param policy 验证码策略
     * @param issuedAt 签发时间
     * @return 成功存储后的结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws VerificationStoreException 当底层存储操作失败时
     */
    StoreResult store(VerificationKey key, String code, VerificationPolicy policy, Instant issuedAt)
            throws VerificationStoreException;

    /**
     * 原子地校验验证码；成功或次数耗尽时消费记录，不匹配时扣减剩余尝试次数。
     *
     *
     * @param key 验证码键
     * @param code 待校验的明文验证码
     * @param verifiedAt 校验时间
     * @return 校验结果
     * @throws NullPointerException 当任一参数为 {@code null} 时
     * @throws VerificationStoreException 当底层存储操作失败时
     */
    VerifyResult verifyAndConsume(VerificationKey key, String code, Instant verifiedAt)
            throws VerificationStoreException;

    /**
     * 当验证码键与明文验证码同时匹配时原子地删除记录。
     *
     *
     * @param key 验证码键
     * @param code 待失效的明文验证码
     * @return 是否删除了匹配记录
     * @throws NullPointerException 当验证码键或验证码为 {@code null} 时
     * @throws VerificationStoreException 当底层存储操作失败时
     */
    boolean invalidate(VerificationKey key, String code) throws VerificationStoreException;
}
