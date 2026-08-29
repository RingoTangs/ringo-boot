package io.github.ringotangs.ringoboot.autoconfigure.verification;

import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemDetailFactory;
import io.github.ringotangs.ringoboot.autoconfigure.problem.ProblemMessageResolver;
import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.verification.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.InvalidVerificationCodeException;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitExceededException;
import io.github.ringotangs.ringoboot.verification.limit.MissingIssueRateLimitRuleException;
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

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * 使用问题消息解析器创建验证码异常处理器。
     *
     * @param messageResolver 问题消息解析器
     */
    public VerificationExceptionHandler(ProblemMessageResolver messageResolver) {
        this.problemDetailFactory = new ProblemDetailFactory(messageResolver);
    }

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
        ProblemType problemType =
                switch (exception) {
                    case CodeGenerationException ignored -> VerificationProblemType.GENERATION_FAILED;
                    case MissingIssueRateLimitRuleException ignored -> VerificationProblemType.CONFIGURATION_ERROR;
                    default -> VerificationProblemType.SERVICE_UNAVAILABLE;
                };
        return problemDetailFactory.create(ProblemException.withCause(problemType, exception));
    }

    /**
     * 将签发限流转换为包含等待秒数的 429 Problem Details。
     *
     * @param exception 签发额度超限异常
     * @return 通过响应头提供精确等待时间并包含友好 Problem Details 的限流响应
     */
    @ExceptionHandler(IssueRateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleIssueRateLimitExceeded(IssueRateLimitExceededException exception) {
        if (logger.isDebugEnabled()) {
            logger.debug("Verification code issuance throttled: violations=" + exception.violations());
        }
        long seconds = retryAfterSeconds(exception.retryAfter());
        ProblemDetail problem = problemDetailFactory.create(ProblemException.withArguments(
                VerificationProblemType.THROTTLED,
                seconds,
                Long.toString(seconds),
                Long.toString(ceilDiv(seconds, 60L)),
                Long.toString(ceilDiv(seconds, 3_600L)),
                Long.toString(ceilDiv(seconds, 86_400L))));
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
        return problemDetailFactory.create(new ProblemException(VerificationProblemType.INVALID_CODE));
    }

    private long retryAfterSeconds(java.time.Duration retryAfter) {
        long seconds = retryAfter.toSeconds();
        return retryAfter.minusSeconds(seconds).isZero() || seconds == Long.MAX_VALUE ? seconds : seconds + 1L;
    }

    private long ceilDiv(long value, long divisor) {
        return value == 0L ? 0L : 1L + (value - 1L) / divisor;
    }
}
