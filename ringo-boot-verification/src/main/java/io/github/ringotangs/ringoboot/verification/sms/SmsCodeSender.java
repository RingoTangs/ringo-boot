package io.github.ringotangs.ringoboot.verification.sms;

import io.github.ringotangs.ringoboot.verification.sender.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;

/**
 * 通过短信渠道派发验证码。
 *
 *
 * <p><strong>API 注意事项：</strong> 实现不得记录或长期保留明文验证码。
 */
@FunctionalInterface
public interface SmsCodeSender {

    /**
     * 将验证码派发到指定手机号码。
     *
     * @param message 短信验证码消息
     * @return 供应商接受状态
     * @throws CodeSenderException 当短信派发操作失败时
     */
    CodeSendResult send(SmsCodeMessage message) throws CodeSenderException;
}
