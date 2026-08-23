package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import java.util.Objects;

/** 解析 Problem Details 响应使用的标题和详情。 */
@FunctionalInterface
public interface ProblemMessageResolver {

    /**
     * 解析指定问题异常的响应消息。
     *
     * @param exception 问题异常
     * @return 已解析的响应消息
     */
    ProblemMessages resolve(ProblemException exception);

    /**
     * 保存已解析的 Problem Details 标题和详情。
     *
     * @param title 问题标题
     * @param detail 问题详情
     */
    record ProblemMessages(String title, String detail) {

        /** 校验已解析消息不为 {@code null}。 */
        public ProblemMessages {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(detail, "detail must not be null");
        }
    }
}
