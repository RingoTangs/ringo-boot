package io.github.ringotangs.ringoboot.autoconfigure.verification;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** 仅在默认签发间隔不为零时注册默认冷却规则。 */
final class OnPositiveIssueRateLimitIntervalCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Duration interval = Binder.get(context.getEnvironment())
                .bind(IssueRateLimitProperties.PREFIX + ".interval", Duration.class)
                .orElse(Duration.ofSeconds(60));
        return interval.isZero()
                ? ConditionOutcome.noMatch("issue rate limit interval is zero")
                : ConditionOutcome.match("issue rate limit interval is non-zero");
    }
}
