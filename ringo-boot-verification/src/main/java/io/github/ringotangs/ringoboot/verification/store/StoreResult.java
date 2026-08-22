package io.github.ringotangs.ringoboot.verification.store;

import java.time.Instant;
import java.util.Objects;

/**
 * 表示验证码状态成功保存后的结果。
 *
 * @param expiresAt 验证码过期时间
 */
public record StoreResult(Instant expiresAt) {

    /**
     * 创建并校验存储结果。
     *
     * @throws NullPointerException 当过期时间为 {@code null} 时
     */
    public StoreResult {
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
