package io.github.ringotangs.springcommons.core;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 表示可预期的问题，并携带用于构建 RFC 9457 Problem Details 响应的问题类型。
 * 异常消息保存格式化后的非国际化默认详情，供日志记录使用；异常处理层可在构建
 * 具体 HTTP 错误响应时对其进行国际化。
 *
 * <p>Represents an expected problem and carries the problem type used to build
 * an RFC 9457 Problem Details response. The exception message contains the formatted,
 * non-localized default detail for logging, while an exception handler may localize it
 * when building the concrete HTTP error response.</p>
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
     * 用于格式化默认详情的不可变消息参数。
     */
    private final List<Object> detailArguments;

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
        this(problemType, null, List.of());
    }

    private ProblemException(ProblemType problemType, @Nullable Throwable cause, List<Object> detailArguments) {
        super(formatDefaultDetail(problemType, detailArguments), cause);
        this.problemType = problemType;
        this.detailArguments = List.copyOf(detailArguments);
    }

    /**
     * 使用问题类型的默认详情和非空原始异常创建问题异常。
     *
     * <p>Creates a problem exception using the problem type's default detail and a
     * non-null original cause.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param cause       非空原始异常 / the non-null original cause
     * @return 问题异常 / the problem exception
     * @throws NullPointerException 当问题类型或原始异常为 {@code null} 时 /
     *                              if the problem type or cause is {@code null}
     */
    public static ProblemException withCause(ProblemType problemType, Throwable cause) {
        return new ProblemException(problemType, Objects.requireNonNull(cause, "cause must not be null"), List.of());
    }

    /**
     * 使用问题类型和非空消息参数创建问题异常。
     *
     * <p>Creates a problem exception with non-null detail message arguments.</p>
     *
     * @param problemType     问题类型 / the problem type
     * @param detailArguments 非空详情消息参数 / the non-null detail message arguments
     * @return 问题异常 / the problem exception
     */
    public static ProblemException withArguments(ProblemType problemType, Object... detailArguments) {
        return new ProblemException(problemType, null, copyArguments(detailArguments));
    }

    /**
     * 使用问题类型、原始异常和非空消息参数创建问题异常。
     *
     * <p>Creates a problem exception with an original cause and non-null detail message
     * arguments.</p>
     *
     * @param problemType     问题类型 / the problem type
     * @param cause           非空原始异常 / the non-null original cause
     * @param detailArguments 非空详情消息参数 / the non-null detail message arguments
     * @return 问题异常 / the problem exception
     */
    public static ProblemException withArgumentsAndCause(
            ProblemType problemType, Throwable cause, Object... detailArguments) {
        return new ProblemException(
                problemType, Objects.requireNonNull(cause, "cause must not be null"), copyArguments(detailArguments));
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
     * 返回不可变的详情消息参数。
     *
     * <p>Returns the immutable detail message arguments.</p>
     *
     * @return 详情消息参数 / the detail message arguments
     */
    public List<Object> getDetailArguments() {
        return detailArguments;
    }

    /**
     * 校验问题类型，并使用消息参数格式化默认详情。
     *
     * <p>Validates the problem type and formats its default detail with message arguments.</p>
     */
    private static String formatDefaultDetail(ProblemType problemType, List<Object> detailArguments) {
        ProblemType requiredProblemType = Objects.requireNonNull(problemType, "problemType must not be null");
        String defaultDetail = requiredProblemType.getDefaultDetail();
        return detailArguments.isEmpty()
                ? defaultDetail
                : new MessageFormat(defaultDetail, Locale.ROOT).format(detailArguments.toArray());
    }

    private static List<Object> copyArguments(Object[] detailArguments) {
        Objects.requireNonNull(detailArguments, "detailArguments must not be null");
        return List.of(detailArguments);
    }
}
