package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 根据 ProblemException 创建 Problem Details。
 *
 * <p>Creates Problem Details from {@link ProblemException} instances.</p>
 */
public final class ProblemDetailFactory {

    private final ProblemMessageResolver messageResolver;

    /**
     * 使用指定消息解析器创建 Problem Details 工厂。
     *
     * @param messageResolver 问题消息解析器
     * @throws NullPointerException 当消息解析器为 {@code null} 时
     */
    public ProblemDetailFactory(ProblemMessageResolver messageResolver) {
        this.messageResolver = Objects.requireNonNull(messageResolver, "messageResolver must not be null");
    }

    /**
     * 根据问题异常创建 Problem Details。
     *
     * @param exception 问题异常
     * @return 包含类型、状态、标题和详情的 Problem Details
     * @throws NullPointerException 当问题异常为 {@code null} 时
     */
    public ProblemDetail create(ProblemException exception) {
        Objects.requireNonNull(exception, "exception must not be null");
        ProblemTypeDefinition definition = exception.getProblemType().getDefinition();
        ProblemMessageResolver.ProblemMessages messages = messageResolver.resolve(exception);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(definition.httpStatus()), messages.detail());
        problem.setType(definition.type());
        problem.setTitle(messages.title());
        return problem;
    }
}
