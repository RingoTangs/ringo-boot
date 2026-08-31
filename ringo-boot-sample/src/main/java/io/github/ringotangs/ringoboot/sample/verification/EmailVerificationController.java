package io.github.ringotangs.ringoboot.sample.verification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示邮箱验证码签发和校验接口。
 *
 * <p>Demonstrates email verification code issuance and verification endpoints.</p>
 */
@RestController
@RequestMapping("/verification/email")
public class EmailVerificationController {

    private final EmailVerificationApplicationService service;

    EmailVerificationController(EmailVerificationApplicationService service) {
        this.service = service;
    }

    /**
     * 签发并发送邮箱验证码。
     *
     * <p>Issues and sends an email verification code.</p>
     *
     * @param request 签发请求 / the issuance request
     * @return 验证码过期时间 / the code expiration instant
     */
    @PostMapping("/code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IssueEmailCodeResponse issue(@Valid @RequestBody IssueEmailCodeRequest request) {
        return new IssueEmailCodeResponse(service.issue(request.email()));
    }

    /**
     * 校验并消费邮箱验证码。
     *
     * <p>Verifies and consumes an email verification code.</p>
     *
     * @apiNote 此端点只用于演示验证码机制。生产应用应在登录、绑定或找回密码等业务用例中校验验证码，并在
     *     成功后立即完成目标操作。 / This endpoint demonstrates the verification mechanism only.
     *     Production applications should verify the code inside a login, binding, or recovery use
     *     case and immediately complete the target operation after success.
     *
     * @param request 校验请求 / the verification request
     */
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyEmailCodeRequest request) {
        service.verify(request.email(), request.code());
    }

    /**
     * 邮箱验证码签发请求。
     *
     * <p>Request to issue an email verification code.</p>
     *
     * @param email 目标邮箱 / the destination email address
     */
    @SuppressWarnings("unused")
    public record IssueEmailCodeRequest(@NotBlank @Email String email) {

        /**
         * 创建请求并去除邮箱首尾空白。
         *
         * <p>Creates the request and strips surrounding whitespace from the email
         * address.</p>
         */
        public IssueEmailCodeRequest {
            //noinspection ConstantValue -- JSON binding can supply null before Bean Validation runs.
            email = email == null ? null : email.strip();
        }
    }

    /**
     * 邮箱验证码签发响应。
     *
     * <p>Response for an issued email verification code.</p>
     *
     * @param expiresAt 验证码过期时间 / the code expiration instant
     */
    public record IssueEmailCodeResponse(Instant expiresAt) {}

    /**
     * 邮箱验证码校验请求。
     *
     * <p>Request to verify an email verification code.</p>
     *
     * @param email 目标邮箱 / the destination email address
     * @param code 六位数字验证码 / the six-digit verification code
     */
    @SuppressWarnings("unused")
    public record VerifyEmailCodeRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code) {

        /**
         * 创建请求并去除邮箱首尾空白。
         *
         * <p>Creates the request and strips surrounding whitespace from the email
         * address.</p>
         */
        public VerifyEmailCodeRequest {
            //noinspection ConstantValue -- JSON binding can supply null before Bean Validation runs.
            email = email == null ? null : email.strip();
        }
    }
}
