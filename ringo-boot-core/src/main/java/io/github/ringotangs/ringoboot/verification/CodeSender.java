package io.github.ringotangs.ringoboot.verification;

/**
 * 将验证码交付给目标主体。
 *
 * <p>Delivers a verification code to its target subject.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or retain the
 *     plaintext code beyond delivery.
 */
@FunctionalInterface
public interface CodeSender {

    /**
     * 发送一次验证码交付。
     *
     * <p>Sends a verification code delivery.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     * @throws NullPointerException 当交付内容为 {@code null} 时 / if the delivery is {@code null}
     */
    void send(CodeDelivery delivery);
}
