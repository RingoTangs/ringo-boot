package io.github.ringotangs.ringoboot.verification.channel.email;

import io.github.ringotangs.ringoboot.verification.channel.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.channel.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.time.Instant;

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
     * @param context 邮件签发上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 供应商接受状态
     * @throws CodeSenderException 当邮件派发操作失败时
     */
    CodeSendResult send(IssueContext context, String code, Instant expiresAt) throws CodeSenderException;
}
