package io.github.ringotangs.ringoboot.verification.limit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 表示一条签发限流规则用于累计额度的业务身份。
 *
 * <p>额度桶由一个或多个有序字符串分段组成。使用分段而不是由规则自行拼接字符串，可以避免分隔符冲突，并允许限流状态存储统一进行
 * 长度编码和 HMAC 处理。分段可能包含手机号、邮箱或 IP 等敏感值，调用方不得直接记录 {@link #segments()}。
 *
 * <p>该记录不可变，构造时会复制分段集合。{@link #toString()} 只输出分段数量。
 *
 * @param segments 构成额度身份的有序分段
 */
public record IssueLimitBucket(List<String> segments) {

    /**
     * 创建额度桶并防御性复制全部分段。
     *
     * @throws NullPointerException 当分段集合或任一分段为 {@code null} 时
     * @throws IllegalArgumentException 当分段集合为空或任一分段为空白时
     */
    public IssueLimitBucket {
        Objects.requireNonNull(segments, "segments must not be null");
        segments = List.copyOf(segments);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
        for (String segment : segments) {
            if (segment.isBlank()) {
                throw new IllegalArgumentException("bucket segment must not be blank");
            }
        }
    }

    /**
     * 使用一个或多个分段创建额度桶。
     *
     * @param segments 构成额度身份的有序分段
     * @return 不可变额度桶
     * @throws NullPointerException 当分段数组或任一分段为 {@code null} 时
     * @throws IllegalArgumentException 当未提供分段或任一分段为空白时
     */
    public static IssueLimitBucket of(String... segments) {
        Objects.requireNonNull(segments, "segments must not be null");
        return new IssueLimitBucket(Arrays.asList(segments));
    }

    /**
     * 返回不包含原始分段值的诊断字符串。
     *
     * @return 只包含分段数量的字符串
     */
    @Override
    public String toString() {
        return "IssueLimitBucket[segmentCount=" + segments.size() + ']';
    }
}
