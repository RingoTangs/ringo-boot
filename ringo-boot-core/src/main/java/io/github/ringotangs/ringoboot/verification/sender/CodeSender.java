package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 派发已生成的验证码。
 *
 * <p>Dispatches a generated verification code.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 * @implSpec 第三方适配器必须将连接、超时、限流和供应商 SDK 故障包装为
 *     {@link CodeSenderException}，不得向调用方泄露供应商异常。 / Third-party adapters must wrap
 *     connection, timeout, rate-limit, and provider SDK failures in
 *     {@link CodeSenderException} instead of exposing vendor exceptions.
 */
@FunctionalInterface
public interface CodeSender {

    /**
     * 将验证码派发到交付键中指定的目标。
     *
     * <p>Dispatches a verification code to the target identified by the delivery
     * key.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     * @throws CodeSenderException 当渠道派发操作失败时 / if the channel delivery operation fails
     */
    void send(CodeDelivery delivery) throws CodeSenderException;
}
