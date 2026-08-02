package io.github.ringotangs.springcommons.core;

import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
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

    /** 显式问题详情；为 {@code null} 时由异常处理层解析默认详情消息。 */
    private final @Nullable String detailOverride;

    /** 用于格式化默认详情的不可变消息参数。 */
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
        this(problemType, null, null, List.of());
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
        this(problemType, detail, null, List.of());
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
        this(problemType, detail, cause, List.of());
    }

    private ProblemException(
            ProblemType problemType,
            @Nullable String detail,
            @Nullable Throwable cause,
            List<Object> detailArguments
    ) {
        super(resolveMessage(problemType, detail, detailArguments), cause);
        this.problemType = problemType;
        this.detailOverride = normalizeDetail(detail);
        this.detailArguments = List.copyOf(detailArguments);
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
                Objects.requireNonNull(cause, "cause must not be null"),
                List.of()
        );
    }

    /**
     * 使用问题类型和非空消息参数创建问题异常。
     *
     * <p>Creates a problem exception with non-null detail message arguments.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param detailArguments 非空详情消息参数 / the non-null detail message arguments
     * @return 问题异常 / the problem exception
     */
    public static ProblemException withArguments(
            ProblemType problemType,
            Object... detailArguments
    ) {
        return new ProblemException(
                problemType,
                null,
                null,
                copyArguments(detailArguments)
        );
    }

    /**
     * 使用问题类型、原始异常和非空消息参数创建问题异常。
     *
     * <p>Creates a problem exception with an original cause and non-null detail message
     * arguments.</p>
     *
     * @param problemType 问题类型 / the problem type
     * @param cause 非空原始异常 / the non-null original cause
     * @param detailArguments 非空详情消息参数 / the non-null detail message arguments
     * @return 问题异常 / the problem exception
     */
    public static ProblemException withArgumentsAndCause(
            ProblemType problemType,
            Throwable cause,
            Object... detailArguments
    ) {
        return new ProblemException(
                problemType,
                null,
                Objects.requireNonNull(cause, "cause must not be null"),
                copyArguments(detailArguments)
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
     * 返回显式问题详情；未提供时返回 {@code null}。
     *
     * <p>Returns the explicit problem detail, or {@code null} when none was supplied.</p>
     *
     * @return 显式问题详情 / the explicit problem detail
     */
    public @Nullable String getDetailOverride() {
        return detailOverride;
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
     * 校验问题类型，并在自定义详情缺失或为空白时返回默认详情。
     *
     * <p>Validates the problem type, then returns its default detail when the custom detail
     * is absent or blank.</p>
     */
    private static String resolveMessage(
            ProblemType problemType,
            @Nullable String detail,
            List<Object> detailArguments
    ) {
        ProblemType requiredProblemType = Objects.requireNonNull(
                problemType,
                "problemType must not be null"
        );
        String detailOverride = normalizeDetail(detail);
        if (detailOverride != null) {
            return detailOverride;
        }
        String defaultDetail = requiredProblemType.getDefaultDetail();
        return detailArguments.isEmpty()
                ? defaultDetail
                : new MessageFormat(defaultDetail, Locale.ROOT)
                        .format(detailArguments.toArray());
    }

    private static @Nullable String normalizeDetail(@Nullable String detail) {
        return detail == null || detail.isBlank() ? null : detail;
    }

    private static List<Object> copyArguments(Object[] detailArguments) {
        Objects.requireNonNull(detailArguments, "detailArguments must not be null");
        return List.of(detailArguments);
    }
}
