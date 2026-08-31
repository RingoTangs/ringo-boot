package io.github.ringotangs.ringoboot.verification.limit;

import io.github.ringotangs.ringoboot.verification.VerificationChannel;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 生成并校验签发限流规则的稳定标识。
 */
final class IssueLimitRuleId {

    private static final String TOKEN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    private static final Pattern CUSTOM_ID = Pattern.compile(TOKEN);
    private static final Pattern GENERATED_ID = Pattern.compile("rule:(?:namespace-quota:ns:"
            + TOKEN
            + ":channel:"
            + TOKEN
            + "|(?:purpose|subject|client-ip)-quota:ns:"
            + TOKEN
            + ":purpose:"
            + TOKEN
            + ":channel:"
            + TOKEN
            + "):issues:[1-9][0-9]*:window:[1-9][0-9]*(?:days|hours|minutes|seconds|milliseconds|nanoseconds)");

    private static final BigInteger NANOS_PER_MILLISECOND = BigInteger.valueOf(1_000_000L);
    private static final BigInteger NANOS_PER_SECOND = BigInteger.valueOf(1_000_000_000L);
    private static final List<WindowUnit> WINDOW_UNITS = List.of(
            new WindowUnit(NANOS_PER_SECOND.multiply(BigInteger.valueOf(86_400L)), "days"),
            new WindowUnit(NANOS_PER_SECOND.multiply(BigInteger.valueOf(3_600L)), "hours"),
            new WindowUnit(NANOS_PER_SECOND.multiply(BigInteger.valueOf(60L)), "minutes"),
            new WindowUnit(NANOS_PER_SECOND, "seconds"),
            new WindowUnit(NANOS_PER_MILLISECOND, "milliseconds"),
            new WindowUnit(BigInteger.ONE, "nanoseconds"));

    private IssueLimitRuleId() {}

    static String namespaceQuota(String namespace, VerificationChannel channel, int maxIssues, Duration window) {
        return generate("namespace-quota", namespace, null, channel, maxIssues, window);
    }

    static String purposeQuota(
            String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window) {
        return generate("purpose-quota", namespace, purpose, channel, maxIssues, window);
    }

    static String subjectQuota(
            String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window) {
        return generate("subject-quota", namespace, purpose, channel, maxIssues, window);
    }

    static String clientIpQuota(
            String namespace, String purpose, VerificationChannel channel, int maxIssues, Duration window) {
        return generate("client-ip-quota", namespace, purpose, channel, maxIssues, window);
    }

    static void validate(String name, String value) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, name + " must not be null");
        if (!CUSTOM_ID.matcher(value).matches() && !GENERATED_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be kebab-case or a generated issue limit rule id: " + value);
        }
    }

    private static String generate(
            String type,
            String namespace,
            String purpose,
            VerificationChannel channel,
            int maxIssues,
            Duration window) {
        StringBuilder id =
                new StringBuilder("rule:").append(type).append(":ns:").append(namespace);
        if (purpose != null) {
            id.append(":purpose:").append(purpose);
        }
        return id.append(":channel:")
                .append(channel.value())
                .append(":issues:")
                .append(maxIssues)
                .append(":window:")
                .append(formatWindow(window))
                .toString();
    }

    private static String formatWindow(Duration window) {
        Objects.requireNonNull(window, "window must not be null");
        BigInteger totalNanos = BigInteger.valueOf(window.getSeconds())
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(window.getNano()));
        if (totalNanos.signum() <= 0) {
            throw new IllegalArgumentException("window must be positive: " + window);
        }
        for (WindowUnit unit : WINDOW_UNITS) {
            BigInteger[] quotientAndRemainder = totalNanos.divideAndRemainder(unit.nanos());
            if (quotientAndRemainder[1].signum() == 0) {
                return quotientAndRemainder[0] + unit.suffix();
            }
        }
        throw new IllegalStateException("positive duration cannot be represented in nanoseconds: " + window);
    }

    private record WindowUnit(BigInteger nanos, String suffix) {}
}
