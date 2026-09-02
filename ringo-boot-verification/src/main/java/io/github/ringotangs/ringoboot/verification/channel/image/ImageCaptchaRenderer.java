package io.github.ringotangs.ringoboot.verification.channel.image;

import io.github.ringotangs.ringoboot.verification.context.IssueContext;

/**
 * 将明文验证码同步渲染为图片。
 *
 * <p>实现不应记录或保留传入的明文验证码，并应将渲染失败包装为 {@link CaptchaRenderingException}。
 */
@FunctionalInterface
public interface ImageCaptchaRenderer {

    /**
     * 渲染验证码图片。
     *
     * @param context 当前签发上下文
     * @param code    仅供本次渲染使用的明文验证码
     * @return 非空验证码图片
     * @throws CaptchaRenderingException 当图片渲染或编码失败时
     */
    CaptchaImage render(IssueContext context, String code) throws CaptchaRenderingException;
}
