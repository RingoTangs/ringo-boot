package io.github.ringotangs.ringoboot.verification.web;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import io.github.ringotangs.ringoboot.problem.web.ProblemDetails;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.channel.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueLimitRuleException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将已知的验证码业务异常和技术异常转换为安全、稳定的 Problem Details 响应。
 *
 * <p>原始异常仅写入服务端日志，响应不会暴露存储实现、发送渠道或供应商诊断信息。</p>
 */
@RestControllerAdvice
@Order(0)
public class VerificationExceptionHandler {

    private static final Log logger = LogFactory.getLog(VerificationExceptionHandler.class);

    /**
     * 记录未由专用方法处理的验证码异常，并构建不包含内部诊断信息的响应。
     *
     * @param exception 验证码异常
     * @return 安全的 Problem Details 响应
     */
    @ExceptionHandler(VerificationException.class)
    public ProblemDetail handleVerificationException(VerificationException exception) {
        if (exception instanceof CodeSenderException senderException) {
            logger.error("Verification code delivery failed: channel=" + senderException.channel(), exception);
        } else {
            logger.error("Verification operation failed", exception);
        }
        ProblemDescriptor descriptor =
                switch (exception) {
                    case CodeGenerationException ignored -> VerificationProblems.GENERATION_FAILED;
                    case MissingIssueLimitRuleException ignored -> VerificationProblems.CONFIGURATION_ERROR;
                    default -> VerificationProblems.SERVICE_UNAVAILABLE;
                };
        return ProblemDetails.create(descriptor, descriptor.detail());
    }

    /**
     * 将签发限流转换为包含等待秒数的 429 Problem Details。
     *
     * @param exception 签发额度超限异常
     * @return 通过响应头提供精确等待时间并包含友好 Problem Details 的限流响应
     */
    @ExceptionHandler(IssueLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleIssueLimitExceeded(IssueLimitExceededException exception) {
        if (logger.isDebugEnabled()) {
            logger.debug("Verification code issuance throttled: violations=" + exception.violations());
        }
        long seconds = retryAfterSeconds(exception.retryAfter());
        ProblemDetail problem = ProblemDetails.create(VerificationProblems.THROTTLED, retryAfterDetail(seconds));
        return ResponseEntity.status(problem.getStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(problem);
    }

    /**
     * 将所有未通过的验证码校验转换为统一的安全响应。
     *
     * @param exception 验证码无效异常
     * @return 验证码无效 Problem Details
     */
    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ProblemDetail handleInvalidVerificationCode(InvalidVerificationCodeException exception) {
        return ProblemDetails.create(VerificationProblems.INVALID_CODE, VerificationProblems.INVALID_CODE.detail());
    }

    private long retryAfterSeconds(java.time.Duration retryAfter) {
        long seconds = retryAfter.toSeconds();
        return retryAfter.minusSeconds(seconds).isZero() || seconds == Long.MAX_VALUE ? seconds : seconds + 1L;
    }

    private String retryAfterDetail(long seconds) {
        if (seconds == 0L) {
            return "Please retry shortly";
        }
        if (seconds == 1L) {
            return "Please retry after 1 second";
        }
        if (seconds < 90L) {
            return "Please retry after approximately " + seconds + " seconds";
        }
        if (seconds < 5_400L) {
            return "Please retry after approximately " + ceilDiv(seconds, 60L) + " minutes";
        }
        if (seconds < 129_600L) {
            return "Please retry after approximately " + ceilDiv(seconds, 3_600L) + " hours";
        }
        return "Please retry after approximately " + ceilDiv(seconds, 86_400L) + " days";
    }

    private long ceilDiv(long value, long divisor) {
        return value == 0L ? 0L : 1L + (value - 1L) / divisor;
    }
}
