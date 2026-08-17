package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.sender.CodeSender;

/**
 * 通过短信渠道派发验证码。
 *
 * <p>Dispatches verification codes through an SMS channel.</p>
 *
 * <p><strong>API 注意事项 / API note:</strong> 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 */
@FunctionalInterface
public interface SmsCodeSender extends CodeSender {}
