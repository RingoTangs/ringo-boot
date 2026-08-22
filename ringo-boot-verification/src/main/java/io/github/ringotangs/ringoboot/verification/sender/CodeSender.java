package io.github.ringotangs.ringoboot.verification.sender;

/**
 * 派发已生成的验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 实现不得记录或长期保留明文验证码。
 * <p><strong>实现要求：</strong> 第三方适配器必须将供应商正常受理映射为 {@link CodeSendResult#ACCEPTED}，明确拒绝映射为
 *     {@link CodeSendResult#REJECTED}，请求超时或响应丢失等不确定结果映射为
 *     {@link CodeSendResult#UNKNOWN}。只有能够确定请求未提交给供应商的本地或适配器故障才应抛出
 *     {@link CodeSenderException}，并且不得泄露供应商异常。
 */
@FunctionalInterface
public interface CodeSender<D> {

    /**
     * 将验证码派发到交付键中指定的目标。
     *
     *
     * @param delivery 渠道专用的验证码交付内容
     * @return 供应商接受状态
     * @throws CodeSenderException 当渠道派发操作失败时
     */
    CodeSendResult send(D delivery) throws CodeSenderException;
}
