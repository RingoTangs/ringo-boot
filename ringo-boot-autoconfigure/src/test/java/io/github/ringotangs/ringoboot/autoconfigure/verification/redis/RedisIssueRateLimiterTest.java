package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
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

class RedisIssueRateLimiterTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void mapsAllowedAndThrottledScriptResults() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L), List.of(1L, 42_000L));
        RedisIssueRateLimiter limiter = limiter(redisTemplate, Duration.ofMinutes(1));

        assertThat(limiter.acquire(KEY, NOW)).isInstanceOf(IssueLimitResult.Allowed.class);
        IssueLimitResult.Throttled throttled = (IssueLimitResult.Throttled) limiter.acquire(KEY, NOW.plusSeconds(18));

        assertThat(throttled.retryAfter()).isEqualTo(Duration.ofSeconds(42));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void hashesSubjectAndUsesDedicatedApplicationKey() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L));
        RedisIssueRateLimiter limiter = limiter(redisTemplate, Duration.ofMinutes(1));

        limiter.acquire(KEY, NOW);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertThat(keys.getValue().getFirst())
                .startsWith("test-application:verification:issue-limit:v1:account:login:")
                .doesNotContain(KEY.subject());
    }

    @Test
    void zeroIntervalBypassesRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertThat(limiter(redisTemplate, Duration.ZERO).acquire(KEY, NOW))
                .isInstanceOf(IssueLimitResult.Allowed.class);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void wrapsRedisFailures() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));

        assertThatThrownBy(() -> limiter(redisTemplate, Duration.ofMinutes(1)).acquire(KEY, NOW))
                .isInstanceOf(IssueRateLimitException.class)
                .hasMessage("Redis issue rate limit operation failed");
    }

    @Test
    void validatesConstructionArguments() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertThatThrownBy(() ->
                        new RedisIssueRateLimiter(redisTemplate, new byte[31], Duration.ofMinutes(1), "application"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        new RedisIssueRateLimiter(redisTemplate, new byte[32], Duration.ofNanos(1), "application"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisIssueRateLimiter(
                        redisTemplate, new byte[32], Duration.ofMinutes(1), "invalid application"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RedisIssueRateLimiter limiter(StringRedisTemplate redisTemplate, Duration interval) {
        return new RedisIssueRateLimiter(redisTemplate, new byte[32], interval, "test-application");
    }
}
