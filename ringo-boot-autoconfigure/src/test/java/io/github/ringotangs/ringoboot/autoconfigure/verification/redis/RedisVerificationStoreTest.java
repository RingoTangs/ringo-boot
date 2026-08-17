package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.VerificationResult;
import io.github.ringotangs.ringoboot.verification.store.StoreResult;
import io.github.ringotangs.ringoboot.verification.store.VerificationStoreException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisVerificationStoreTest {

    private static final VerificationKey KEY = new VerificationKey("account", "email-verification", "user@example.com");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void mapsStoreAndVerificationScriptResults() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(NOW.plusSeconds(300).toEpochMilli(), 2L);
        RedisVerificationStore store = store(redisTemplate);

        StoreResult stored = store.store(KEY, "123456", VerificationPolicy.defaults(), NOW);

        assertThat(stored.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(store.verifyAndConsume(KEY, "123456", NOW.plusSeconds(1))).isEqualTo(VerificationResult.SUCCESS);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void wrapsSpringDataFailureWithoutSensitiveValues() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("provider leaked user@example.com"));
        RedisVerificationStore store = store(redisTemplate);

        assertThatThrownBy(() -> store.verifyAndConsume(KEY, "123456", NOW))
                .isInstanceOf(VerificationStoreException.class)
                .hasMessage("Redis verification operation failed")
                .message()
                .doesNotContain(KEY.subject(), "123456");
    }

    @Test
    void rejectsMissingScriptResults() {
        RedisVerificationStore store = store(mock(StringRedisTemplate.class));

        assertThatThrownBy(() -> store.store(KEY, "123456", VerificationPolicy.defaults(), NOW))
                .isInstanceOf(VerificationStoreException.class)
                .hasMessage("Redis store script returned no result");
        assertThatThrownBy(() -> store.verifyAndConsume(KEY, "123456", NOW))
                .isInstanceOf(VerificationStoreException.class)
                .hasMessage("Redis verify script returned no result");
        assertThatThrownBy(() -> store.invalidate(KEY, "123456"))
                .isInstanceOf(VerificationStoreException.class)
                .hasMessage("Redis invalidate script returned no result");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void doesNotSendPlaintextSubjectOrCodeToRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(NOW.plusSeconds(300).toEpochMilli());
        RedisVerificationStore store = store(redisTemplate);

        store.store(KEY, "123456", VerificationPolicy.defaults(), NOW);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), arguments.capture());
        assertThat(keys.getValue().getFirst())
                .startsWith("test-application:verification:v1:account:email-verification:")
                .doesNotContain(KEY.subject());
        assertThat(arguments.getValue())
                .allSatisfy(argument -> assertThat(argument.toString()).doesNotContain(KEY.subject(), "123456"));
    }

    @Test
    void validatesConstructionArguments() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        assertThatThrownBy(() -> new RedisVerificationStore(redisTemplate, new byte[31], Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisVerificationStore(redisTemplate, new byte[32], Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisVerificationStore(
                        redisTemplate, new byte[32], Duration.ofMinutes(1), "invalid application"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationName");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    void keepsLegacyKeyFormatForDeprecatedConstructor() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(NOW.plusSeconds(300).toEpochMilli());
        RedisVerificationStore store = new RedisVerificationStore(redisTemplate, new byte[32], Duration.ofMinutes(1));

        store.store(KEY, "123456", VerificationPolicy.defaults(), NOW);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertThat(keys.getValue().getFirst())
                .startsWith("ringo:verification:v1:account:email-verification:")
                .doesNotContain("test-application");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void isolatesApplicationsInKeysAndDigests() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(NOW.plusSeconds(300).toEpochMilli());

        new RedisVerificationStore(redisTemplate, new byte[32], Duration.ofMinutes(1), "application-one")
                .store(KEY, "123456", VerificationPolicy.defaults(), NOW);
        new RedisVerificationStore(redisTemplate, new byte[32], Duration.ofMinutes(1), "application-two")
                .store(KEY, "123456", VerificationPolicy.defaults(), NOW);

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, org.mockito.Mockito.times(2))
                .execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertThat(keys.getAllValues().get(0).getFirst())
                .startsWith("application-one:verification:v1:")
                .isNotEqualTo(keys.getAllValues().get(1).getFirst());
        assertThat(keys.getAllValues().get(1).getFirst()).startsWith("application-two:verification:v1:");
    }

    private RedisVerificationStore store(StringRedisTemplate redisTemplate) {
        return new RedisVerificationStore(redisTemplate, new byte[32], Duration.ofMinutes(1), "test-application");
    }
}
