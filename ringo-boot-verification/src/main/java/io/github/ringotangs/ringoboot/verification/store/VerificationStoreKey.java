package io.github.ringotangs.ringoboot.verification.store;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import java.util.Objects;

/**
 * 唯一标识一个渠道中的验证码存储记录。
 *
 * @param key     验证码业务键
 * @param channel 验证码渠道
 */
public record VerificationStoreKey(VerificationKey key, VerificationChannel channel) {

    /**
     * 创建并校验验证码存储键。
     */
    public VerificationStoreKey {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
    }

    /**
     * 返回不包含验证主体的诊断字符串。
     *
     * @return 安全的诊断字符串
     */
    @Override
    public String toString() {
        return "VerificationStoreKey[namespace=" + key.namespace() + ", purpose=" + key.purpose() + ", channel="
                + channel + ']';
    }
}
