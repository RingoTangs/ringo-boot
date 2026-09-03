package io.github.ringotangs.ringoboot.problem.web;

import io.github.ringotangs.ringoboot.problem.ProblemDescriptor;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 根据 {@link ProblemDescriptor} 创建 Problem Details。
 */
public final class ProblemDetails {

    private ProblemDetails() {}

    /**
     * 根据问题描述和详情创建 Problem Details。
     *
     * @param descriptor 问题描述
     * @param detail 问题详情
     * @return 包含类型、状态、标题和详情的 Problem Details
     * @throws NullPointerException 当问题描述或详情为 {@code null} 时
     */
    public static ProblemDetail create(ProblemDescriptor descriptor, String detail) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(descriptor.status()), detail);
        problem.setType(descriptor.type());
        problem.setTitle(descriptor.title());
        return problem;
    }
}
