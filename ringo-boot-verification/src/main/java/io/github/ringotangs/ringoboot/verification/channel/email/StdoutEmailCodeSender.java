package io.github.ringotangs.ringoboot.verification.channel.email;

import io.github.ringotangs.ringoboot.verification.channel.CodeSendResult;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.time.Instant;
import java.util.Objects;

/**
 * 将邮件验证码输出到标准输出，适用于开发和演示环境。
 *
 *
 * <p><strong>API 注意事项：</strong> 此实现会输出明文验证码，不应在生产环境使用。
 */
public final class StdoutEmailCodeSender implements EmailCodeSender {

    /** 创建标准输出邮件验证码发送器。 */
    public StdoutEmailCodeSender() {}

    /**
     * 将验证码及脱敏后的邮箱地址输出到标准输出。
     *
     *
     * @param context 邮件签发上下文
     * @param code 仅供发送期间使用的明文验证码
     * @param expiresAt 验证码过期时间
     * @return 始终返回供应商已接受
     */
    @Override
    public CodeSendResult send(IssueContext context, String code, Instant expiresAt) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        System.out.println("DEVELOPMENT ONLY - Email verification code: namespace="
                + context.key().namespace()
                + ", purpose="
                + context.key().purpose()
                + ", email="
                + mask(context.key().subject())
                + ", code="
                + code
                + ", expiresAt="
                + expiresAt);
        return CodeSendResult.ACCEPTED;
    }

    private String mask(String subject) {
        int separator = subject.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return subject.charAt(0) + "***" + subject.substring(separator);
    }
}
