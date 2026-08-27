package io.github.ringotangs.ringoboot.verification.sender;

import io.github.ringotangs.ringoboot.verification.IssueContext;
import java.time.Instant;
import java.util.Objects;

/**
 * 描述一次待发送的验证码交付。
 *
 *
 * @param context 当前签发流程的上下文
 * @param code 仅供发送期间使用的明文验证码
 * @param expiresAt 验证码过期时间
 */
public record CodeDelivery(IssueContext context, String code, Instant expiresAt) {

    /**
     * 创建并校验验证码交付内容。
     *
     *
     * @throws NullPointerException 当任一参数为 {@code null} 时
     */
    public CodeDelivery {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 返回隐藏明文验证码的安全字符串表示。
     *
     *
     * @return 脱敏后的字符串表示
     */
    @Override
    public String toString() {
        return "CodeDelivery[context=" + context + ", code=<redacted>, expiresAt=" + expiresAt + "]";
    }
}
