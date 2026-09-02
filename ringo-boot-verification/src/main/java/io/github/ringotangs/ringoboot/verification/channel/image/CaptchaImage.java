package io.github.ringotangs.ringoboot.verification.channel.image;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 不可变的验证码图片内容。
 */
public final class CaptchaImage {

    private static final Pattern IMAGE_MEDIA_TYPE = Pattern.compile("image/[a-z0-9][a-z0-9.+-]*");

    private final String mediaType;
    private final byte[] content;

    /**
     * 创建验证码图片并防御性复制内容。
     *
     * @param mediaType 图片媒体类型
     * @param content   非空图片字节
     */
    public CaptchaImage(String mediaType, byte[] content) {
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType must not be null");
        if (!IMAGE_MEDIA_TYPE.matcher(mediaType).matches()) {
            throw new IllegalArgumentException("mediaType must be a lowercase image media type: " + mediaType);
        }
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }
        this.content = content.clone();
    }

    /**
     * 返回图片媒体类型。
     */
    public String mediaType() {
        return mediaType;
    }

    /**
     * 返回图片内容的防御性副本。
     */
    public byte[] content() {
        return content.clone();
    }

    /**
     * 返回不包含图片内容的诊断字符串。
     */
    @Override
    public String toString() {
        return "CaptchaImage[mediaType=" + mediaType + ", size=" + content.length + ']';
    }
}
