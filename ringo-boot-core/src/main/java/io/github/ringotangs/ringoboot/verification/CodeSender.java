package io.github.ringotangs.ringoboot.verification;

/**
 * 派发已生成的验证码。
 *
 * <p>Dispatches a generated verification code.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
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
     */
    void send(CodeDelivery delivery);
}
