package io.github.ringotangs.ringoboot.verification.channel;

import java.time.Instant;
import java.util.Objects;

/**
 * 表示邮件或短信验证码成功签发后的投递结果。
 */
public sealed interface DeliveryResult permits DeliveryResult.Accepted, DeliveryResult.Uncertain {

    /**
     * 表示发送供应商明确接受了验证码投递请求。
     *
     * @param expiresAt 验证码过期时间
     */
    record Accepted(Instant expiresAt) implements DeliveryResult {

        public Accepted {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }

    /**
     * 表示验证码已经签发，但无法确认发送供应商是否接受了请求。
     *
     * @param expiresAt 验证码过期时间
     */
    record Uncertain(Instant expiresAt) implements DeliveryResult {

        public Uncertain {
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }
    }
}
