package io.github.ringotangs.ringoboot.problem.web;

import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 根据 {@link ProblemType} 创建 Problem Details。
 */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {}

    /**
     * 根据问题类型和详情创建 Problem Details。
     *
     * @param problemType 问题类型
     * @param detail      问题详情
     * @return 包含类型、状态、标题和详情的 Problem Details
     * @throws NullPointerException 当问题类型或详情为 {@code null} 时
     */
    public static ProblemDetail create(ProblemType problemType, String detail) {
        Objects.requireNonNull(problemType, "problemType must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        ProblemTypeDefinition definition = problemType.getDefinition();
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(definition.httpStatus()), detail);
        problem.setType(definition.type());
        problem.setTitle(definition.title());
        return problem;
    }
}
