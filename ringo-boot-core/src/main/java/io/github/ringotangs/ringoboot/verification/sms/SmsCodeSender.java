package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.CodeDelivery;

/**
 * 通过短信渠道派发验证码。
 *
 * <p>Dispatches verification codes through an SMS channel.</p>
 *
 * @apiNote 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 */
@FunctionalInterface
public interface SmsCodeSender {

    /**
     * 将验证码派发到交付键中指定的手机号。
     *
     * <p>Dispatches a verification code to the phone number identified by the delivery
     * key.</p>
     *
     * @param delivery 验证码交付内容 / the verification code delivery
     */
    void send(CodeDelivery delivery);
}
