package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitQuota;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueRateLimitException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisIssueRateLimitStoreTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void mapsAllowedAndThrottledMultiRuleResults() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L), List.of(1L, 42_000L));
        RedisIssueRateLimitStore store = store(redisTemplate);
        List<IssueLimitQuota> quotas = List.of(
                quota("subject-minute", "user@example.com", 1, Duration.ofMinutes(1)),
                quota("ip-hour", "203.0.113.10", 10, Duration.ofHours(1)));

        assertThat(store.acquire(quotas, NOW)).isInstanceOf(IssueLimitResult.Allowed.class);
        IssueLimitResult.Throttled throttled = (IssueLimitResult.Throttled) store.acquire(quotas, NOW.plusSeconds(18));

        assertThat(throttled.retryAfter()).isEqualTo(Duration.ofSeconds(42));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void hashesBucketsAndUsesOneRedisClusterHashTag() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L));
        RedisIssueRateLimitStore store = store(redisTemplate);

        store.acquire(
                List.of(
                        quota("subject-minute", "user@example.com", 1, Duration.ofMinutes(1)),
                        quota("ip-hour", "203.0.113.10", 10, Duration.ofHours(1))),
                NOW);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertThat(keys.getValue())
                .hasSize(2)
                .allSatisfy(key -> assertThat(key)
                        .startsWith(
                                "test-application:verification:issue-limit:{test-application:verification:issue-limit}:v2:")
                        .doesNotContain("user@example.com", "203.0.113.10"));
    }

    @Test
    void emptyConstraintsBypassRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertThat(store(redisTemplate).acquire(List.of(), NOW)).isInstanceOf(IssueLimitResult.Allowed.class);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void wrapsRedisFailures() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));

        assertThatThrownBy(() -> store(redisTemplate)
                        .acquire(List.of(quota("subject-minute", "user", 1, Duration.ofMinutes(1))), NOW))
                .isInstanceOf(IssueRateLimitException.class)
                .hasMessage("Redis issue rate limit operation failed");
    }

    @Test
    void validatesArgumentsAndRedisResolution() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertThatThrownBy(() -> new RedisIssueRateLimitStore(redisTemplate, new byte[31], "application"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisIssueRateLimitStore(redisTemplate, new byte[32], "invalid application"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        store(redisTemplate).acquire(List.of(quota("fast-rule", "user", 1, Duration.ofNanos(1))), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RedisIssueRateLimitStore store(StringRedisTemplate redisTemplate) {
        return new RedisIssueRateLimitStore(redisTemplate, new byte[32], "test-application");
    }

    private IssueLimitQuota quota(String id, String bucket, int maxIssues, Duration window) {
        return new IssueLimitQuota(id, IssueLimitBucket.of(bucket), maxIssues, window);
    }
}
