package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 派发已生成的验证码。
 *
 * <p>Dispatches a generated verification code.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 * @implSpec 第三方适配器必须将供应商正常受理映射为 {@link CodeSendResult#ACCEPTED}，明确拒绝映射为
 *     {@link CodeSendResult#REJECTED}，请求超时或响应丢失等不确定结果映射为
 *     {@link CodeSendResult#UNKNOWN}。只有能够确定请求未提交给供应商的本地或适配器故障才应抛出
 *     {@link CodeSenderException}，并且不得泄露供应商异常。 / Third-party adapters must map
 *     provider acceptance to {@link CodeSendResult#ACCEPTED}, explicit rejection to {@link
 *     CodeSendResult#REJECTED}, and ambiguous outcomes such as request timeouts or lost responses
 *     to {@link CodeSendResult#UNKNOWN}. A {@link CodeSenderException} should be thrown only for a
 *     local or adapter failure known to occur before provider submission, without exposing vendor
 *     exceptions.
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
     * @return 供应商接受状态 / provider acceptance status
     * @throws CodeSenderException 当渠道派发操作失败时 / if the channel delivery operation fails
     */
    CodeSendResult send(CodeDelivery delivery) throws CodeSenderException;
}
