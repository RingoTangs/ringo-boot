package io.github.ringotangs.ringoboot.verification.limit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** 表示一条签发限流规则用于累计额度的业务身份。 */
public record IssueLimitBucket(List<String> segments) {

    /** 创建额度桶并复制所有分段。 */
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

    /** 使用一个或多个分段创建额度桶。 */
    public static IssueLimitBucket of(String... segments) {
        Objects.requireNonNull(segments, "segments must not be null");
        return new IssueLimitBucket(Arrays.asList(segments));
    }

    /** 避免日志意外暴露手机号、邮箱、IP 等额度桶原始值。 */
    @Override
    public String toString() {
        return "IssueLimitBucket[segmentCount=" + segments.size() + ']';
    }
}
