package io.github.ringotangs.ringoboot.sample.verification;

import java.time.Instant;

/**
 * 向指定邮箱交付验证码。
 *
 * <p>Delivers a verification code to an email address.</p>
 */
interface EmailCodeSender {

    /**
     * 发送验证码及其过期时间。
     *
     * <p>Sends a verification code and its expiration instant.</p>
     *
     * @param email 目标邮箱 / the destination email address
     * @param code 明文验证码 / the plaintext verification code
     * @param expiresAt 验证码过期时间 / the expiration instant
     */
    void send(String email, String code, Instant expiresAt);
}
