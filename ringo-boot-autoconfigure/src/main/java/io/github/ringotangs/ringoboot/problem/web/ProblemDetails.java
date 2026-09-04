package io.github.ringotangs.ringoboot.problem.web;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 根据 {@link ProblemDescriptor} 创建 Problem Details。
 */
public final class ProblemDetails {

    private ProblemDetails() {}

    /**
     * 根据问题描述的默认详情创建 Problem Details。
     */
    public static ProblemDetail create(ProblemDescriptor descriptor) {
        return create(descriptor, null);
    }

    /**
     * 根据问题描述和可选的详情格式化函数创建 Problem Details。
     *
     * @param descriptor      问题描述
     * @param detailFormatter 接收默认详情模板并返回最终详情的函数；为 {@code null} 时不格式化
     * @return 包含类型、状态、标题和详情的 Problem Details
     * @throws NullPointerException 当问题描述或格式化结果为 {@code null} 时
     */
    public static ProblemDetail create(ProblemDescriptor descriptor, @Nullable UnaryOperator<String> detailFormatter) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        String detail = descriptor.detail();
        if (detailFormatter != null) {
            detail = Objects.requireNonNull(detailFormatter.apply(detail), "formatted detail must not be null");
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(descriptor.status()), detail);
        problem.setType(descriptor.type());
        problem.setTitle(descriptor.title());
        return problem;
    }
}
