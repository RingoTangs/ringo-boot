package io.github.ringotangs.ringoboot.sample.verification;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 将最新邮件验证码保存在内存中，供示例联调使用。
 *
 * <p>Stores the latest email verification code in memory for sample integration.</p>
 *
 * @apiNote 该实现会保留明文验证码，只能用于本地示例和测试，不能用于生产环境。
 *     / This implementation retains plaintext codes and is only suitable for local
 *     samples and tests, never for production use.
 */
final class InMemoryEmailCodeSender implements EmailCodeSender {

    private final ConcurrentMap<String, EmailCodeMessage> messages = new ConcurrentHashMap<>();

    @Override
    public void send(String email, String code, Instant expiresAt) {
        messages.put(email, new EmailCodeMessage(code, expiresAt));
    }

    Optional<EmailCodeMessage> findLatest(String email) {
        return Optional.ofNullable(messages.get(email));
    }

    record EmailCodeMessage(String code, Instant expiresAt) {}
}
