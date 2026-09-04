package io.github.ringotangs.ringoboot.verification.channel.image;

import java.time.Instant;
import java.util.Objects;

/**
 * 表示图片验证码已经成功签发并渲染。
 *
 * @param expiresAt 验证码过期时间
 * @param image 渲染后的验证码图片
 */
public record ImageCaptchaResult(Instant expiresAt, CaptchaImage image) {

    public ImageCaptchaResult {
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(image, "image must not be null");
    }
}
