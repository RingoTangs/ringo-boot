package io.github.ringotangs.commons.core;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 表示可预期的问题，并携带用于构建 RFC 9457 Problem Details 响应的问题类型。
 * 异常消息对应 Problem Details 的 {@code detail} 字段，异常处理层负责将该异常
 * 转换为具体的 HTTP 错误响应。
 *
 * <p>Represents an expected problem and carries the problem type used to build
 * an RFC 9457 Problem Details response. The exception message corresponds to the
 * {@code detail} member, while an exception handler converts this exception into the
 * concrete HTTP error response.</p>
 *
 * @see ProblemType
 */
public final class ProblemException extends RuntimeException {

    /**
     * 描述该失败的稳定问题类型。
     *
     * <p>The stable problem type that describes this failure.</p>
     */
    private final ProblemType problemType;

    /**
     * 使用问题类型的默认详情创建问题异常。
     *
     * <p>Creates a problem exception using the problem type's default detail.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @throws NullPointerException 当问题类型为 {@code null} 时 /
     *                              if the problem type is {@code null}
     */
    public ProblemException(ProblemType problemType) {
        this(problemType, null, null);
    }

    /**
     * 使用自定义详情创建问题异常；详情为 {@code null} 或空白时使用默认详情。
     *
     * <p>Creates a problem exception with a custom detail. The default detail is used
     * when the supplied detail is {@code null} or blank.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param detail 自定义问题详情 / the custom problem detail
     * @throws NullPointerException 当问题类型为 {@code null} 时 /
     *                              if the problem type is {@code null}
     */
    public ProblemException(ProblemType problemType, @Nullable String detail) {
        this(problemType, detail, null);
    }

    /**
     * 使用自定义详情和原始异常创建问题异常；详情为 {@code null} 或空白时使用默认详情。
     *
     * <p>Creates a problem exception with a custom detail and the original cause. The
     * default detail is used when the supplied detail is {@code null} or blank.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param detail 自定义问题详情 / the custom problem detail
     * @param cause 原始异常，可为 {@code null} / the original cause, which may be {@code null}
     * @throws NullPointerException 当问题类型为 {@code null} 时 /
     *                              if the problem type is {@code null}
     */
    public ProblemException(
            ProblemType problemType,
            @Nullable String detail,
            @Nullable Throwable cause
    ) {
        super(resolveDetail(problemType, detail), cause);
        this.problemType = problemType;
    }

    /**
     * 使用问题类型的默认详情和非空原始异常创建问题异常。
     *
     * <p>Creates a problem exception using the problem type's default detail and a
     * non-null original cause.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param cause 非空原始异常 / the non-null original cause
     * @return 问题异常 / the problem exception
     * @throws NullPointerException 当问题类型或原始异常为 {@code null} 时 /
     *                              if the problem type or cause is {@code null}
     */
    public static ProblemException withCause(ProblemType problemType, Throwable cause) {
        return new ProblemException(
                problemType,
                null,
                Objects.requireNonNull(cause, "cause must not be null")
        );
    }

    /**
     * 返回描述该失败的问题类型。
     *
     * <p>Returns the problem type that describes this failure.</p>
     *
     * @return 问题类型 / the problem type
     */
    public ProblemType getProblemType() {
        return problemType;
    }

    /**
     * 校验问题类型和 HTTP 错误状态，并在自定义详情缺失或为空白时返回默认详情。
     *
     * <p>Validates the problem type and HTTP error status, then returns its default detail
     * when the custom detail is absent or blank.</p>
     */
    private static String resolveDetail(ProblemType problemType, @Nullable String detail) {
        ProblemType requiredProblemType = Objects.requireNonNull(
                problemType,
                "problemType must not be null"
        );
        int httpStatus = requiredProblemType.getHttpStatus();
        if (httpStatus < 400 || httpStatus > 599) {
            throw new IllegalArgumentException(
                    "httpStatus must be between 400 and 599: " + httpStatus
            );
        }
        return detail == null || detail.isBlank()
                ? requiredProblemType.getDefaultDetail()
                : detail;
    }
}
