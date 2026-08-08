package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;

/**
 * 通过邮件渠道派发验证码。
 *
 * <p>Dispatches verification codes through an email channel.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 */
@FunctionalInterface
public interface EmailCodeSender {

    /**
     * 将验证码派发到交付键中指定的邮箱。
     *
     * <p>Dispatches a verification code to the email address identified by the
     * delivery key.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    void send(CodeDelivery delivery);
}
