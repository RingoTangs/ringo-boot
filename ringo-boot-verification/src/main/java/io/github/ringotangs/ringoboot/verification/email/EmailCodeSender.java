package io.github.ringotangs.ringoboot.verification.email;

import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;

/**
 * 通过邮件渠道派发验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 实现不得记录或长期保留明文验证码。
 */
@FunctionalInterface
public interface EmailCodeSender {

    /**
     * 将验证码派发到指定邮箱。
     *
     * @param delivery 邮件验证码交付内容
     * @return 供应商接受状态
     * @throws CodeSenderException 当邮件派发操作失败时
     */
    CodeSendResult send(EmailCodeDelivery delivery) throws CodeSenderException;
}
