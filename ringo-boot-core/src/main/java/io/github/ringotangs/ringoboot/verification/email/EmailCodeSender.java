package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.sender.CodeSender;

/**
 * 通过邮件渠道派发验证码。
 *
 * <p>Dispatches verification codes through an email channel.</p>
 *
 * <p><strong>API 注意事项 / API note:</strong> 实现不得记录或长期保留明文验证码。 / Implementations must not log or
 *     retain plaintext codes beyond delivery.
 */
@FunctionalInterface
public interface EmailCodeSender extends CodeSender {}
