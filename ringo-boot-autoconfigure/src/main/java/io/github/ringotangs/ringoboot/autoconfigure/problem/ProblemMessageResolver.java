package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import java.util.Objects;

/**
 * 解析 Problem Details 响应使用的标题和详情。
 *
 * <p>Resolves the title and detail used by a Problem Details response.</p>
 */
@FunctionalInterface
public interface ProblemMessageResolver {

    /**
     * 解析指定问题异常的响应消息。
     *
     * <p>Resolves the response messages for the given problem exception.</p>
     *
     * @param exception 问题异常 / the problem exception
     * @return 已解析的响应消息 / the resolved response messages
     */
    ProblemMessages resolve(ProblemException exception);

    /**
     * 保存已解析的 Problem Details 标题和详情。
     *
     * <p>Stores a resolved Problem Details title and detail.</p>
     *
     * @param title 问题标题 / the problem title
     * @param detail 问题详情 / the problem detail
     */
    record ProblemMessages(String title, String detail) {

        /** 校验已解析消息不为 {@code null}。 */
        public ProblemMessages {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(detail, "detail must not be null");
        }
    }
}
