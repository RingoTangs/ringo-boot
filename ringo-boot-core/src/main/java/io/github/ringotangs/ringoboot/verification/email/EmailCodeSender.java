package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.sender.CodeSender;

/**
 * 通过邮件渠道派发验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 实现不得记录或长期保留明文验证码。
 */
@FunctionalInterface
public interface EmailCodeSender extends CodeSender {}
