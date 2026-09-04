package io.github.ringotangs.ringoboot.verification.channel.image;

import io.github.ringotangs.ringoboot.verification.AbstractVerificationService;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.channel.VerificationChannel;
import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import io.github.ringotangs.ringoboot.verification.context.IssueContextManager;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerator;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimiter;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.time.Instant;
import java.util.Objects;

/**
 * 统一编排验证码生命周期并同步返回图片验证码。
 */
public final class ImageCaptchaService extends AbstractVerificationService<ImageCaptchaResult> {

    private final ImageCaptchaRenderer renderer;

    /**
     * 使用完整依赖创建图片验证码服务。
     */
    public ImageCaptchaService(
            CodeGenerator codeGenerator,
            VerificationStore store,
            IssueLimiter issueLimiter,
            VerificationPolicy verificationPolicy,
            IssueContextManager issueContextManager,
            ImageCaptchaRenderer renderer) {
        super(codeGenerator, store, issueLimiter, verificationPolicy, issueContextManager);
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
    }

    @Override
    protected ImageCaptchaResult completeIssue(IssueContext context, String code, Instant expiresAt) {
        CaptchaImage image = Objects.requireNonNull(
                renderer.render(context, code), "image captcha renderer result must not be null");
        return new ImageCaptchaResult(expiresAt, image);
    }

    @Override
    protected VerificationChannel channel() {
        return VerificationChannel.IMAGE;
    }
}
