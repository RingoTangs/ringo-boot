package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.verification.VerificationException;
import io.github.ringotangs.ringoboot.verification.generator.CodeGenerationException;
import io.github.ringotangs.ringoboot.verification.sender.CodeSenderException;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将已知的验证码技术异常转换为安全、稳定的 Problem Details 响应。
 *
 * <p>Converts known verification technical failures into safe and stable Problem Details
 * responses.</p>
 *
 * @apiNote 原始异常仅写入服务端日志，响应不会暴露存储实现、发送渠道或供应商诊断信息。 / The
 *     original exception is written only to server logs; responses do not expose storage,
 *     delivery-channel, or provider diagnostics.
 */
@RestControllerAdvice
@Order(0)
public class VerificationExceptionHandler {

    private static final Log logger = LogFactory.getLog(VerificationExceptionHandler.class);

    private final ProblemDetailFactory problemDetailFactory;

    /**
     * 使用问题消息解析器创建验证码异常处理器。
     *
     * <p>Creates a verification exception handler with a problem message resolver.</p>
     *
     * @param messageResolver 问题消息解析器 / the problem message resolver
     */
    public VerificationExceptionHandler(ProblemMessageResolver messageResolver) {
        this.problemDetailFactory = new ProblemDetailFactory(messageResolver);
    }

    /**
     * 记录验证码技术异常并构建不包含内部诊断信息的响应。
     *
     * <p>Logs a verification technical failure and builds a response without internal diagnostic
     * information.</p>
     *
     * @param exception 验证码技术异常 / the verification technical failure
     * @return 安全的 Problem Details 响应 / the safe Problem Details response
     */
    @ExceptionHandler({CodeGenerationException.class, CodeSenderException.class, VerificationStoreException.class})
    public ProblemDetail handleVerificationException(VerificationException exception) {
        logger.error("Verification operation failed", exception);
        ProblemType problemType = exception instanceof CodeGenerationException
                ? VerificationProblemType.GENERATION_FAILED
                : VerificationProblemType.SERVICE_UNAVAILABLE;
        return problemDetailFactory.create(ProblemException.withCause(problemType, exception));
    }
}
