package io.github.ringotangs.ringoboot.verification.email;

import java.time.Instant;

/**
 * 描述一次待发送的邮件验证码。
 */
public record EmailCodeDelivery(String namespace, String purpose, String email, String code, Instant expiresAt) {
}
