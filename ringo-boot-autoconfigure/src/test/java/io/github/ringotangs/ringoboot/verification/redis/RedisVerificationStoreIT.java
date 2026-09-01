package io.github.ringotangs.ringoboot.verification.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitBucket;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitQuota;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitResult;
import io.github.ringotangs.ringoboot.verification.limit.IssueLimitStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisVerificationStoreIT extends VerificationStoreContract {

    private static final int DEFAULT_REDIS_PORT = 6379;
    private static final int DEFAULT_REDIS_DATABASE = 0;

    private static LettuceConnectionFactory connectionFactory;
    private static VerificationStore store;
    private static IssueLimitStore issueLimitStore;

    @BeforeAll
    static void createStore() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                requiredEnvironment("REDIS_IT_HOST"), integerEnvironment("REDIS_IT_PORT", DEFAULT_REDIS_PORT));
        configuration.setDatabase(integerEnvironment("REDIS_IT_DATABASE", DEFAULT_REDIS_DATABASE));
        optionalEnvironment("REDIS_IT_USERNAME").ifPresent(configuration::setUsername);
        optionalEnvironment("REDIS_IT_PASSWORD").map(RedisPassword::of).ifPresent(configuration::setPassword);

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfiguration =
                LettuceClientConfiguration.builder();
        if (booleanEnvironment("REDIS_IT_SSL", false)) {
            clientConfiguration.useSsl();
        }
        connectionFactory = new LettuceConnectionFactory(configuration, clientConfiguration.build());
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        assertConnectionAvailable();

        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        store = new RedisVerificationStore(redisTemplate, secret, Duration.ofMinutes(1), "ringo-boot-redis-it");
        issueLimitStore = new RedisIssueLimitStore(redisTemplate, secret, "ringo-boot-redis-it");
    }

    @AfterAll
    static void closeConnection() {
        connectionFactory.destroy();
    }

    @Override
    protected VerificationStore store() {
        return store;
    }

    @Test
    void limitsConcurrentIssuanceAtomically() throws Exception {
        VerificationKey key =
                new VerificationKey("account", "login", UUID.randomUUID().toString());
        IssueLimitQuota quota = quota(key);
        Instant requestedAt = Instant.now();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(threads)) {
            @SuppressWarnings("unchecked")
            Future<IssueLimitResult>[] futures = new Future[threads];
            for (int index = 0; index < threads; index++) {
                futures[index] = executor.submit(() -> {
                    start.await();
                    return issueLimitStore.acquire(java.util.List.of(quota), requestedAt);
                });
            }
            start.countDown();

            int allowed = 0;
            for (Future<IssueLimitResult> future : futures) {
                if (future.get() instanceof IssueLimitResult.Allowed) {
                    allowed++;
                }
            }
            assertEquals(1, allowed);
        }
    }

    @Test
    void allowsIssuanceAfterRedisTtlExpires() throws Exception {
        VerificationKey key =
                new VerificationKey("account", "registration", UUID.randomUUID().toString());
        IssueLimitQuota quota = quota(key);

        assertInstanceOf(
                IssueLimitResult.Allowed.class, issueLimitStore.acquire(java.util.List.of(quota), Instant.now()));
        assertInstanceOf(
                IssueLimitResult.Throttled.class, issueLimitStore.acquire(java.util.List.of(quota), Instant.now()));
        Thread.sleep(600);

        assertInstanceOf(
                IssueLimitResult.Allowed.class, issueLimitStore.acquire(java.util.List.of(quota), Instant.now()));
    }

    private static IssueLimitQuota quota(VerificationKey key) {
        return new IssueLimitQuota(
                "verification-key-cooldown",
                IssueLimitBucket.of(key.namespace(), key.purpose(), key.subject()),
                1,
                Duration.ofMillis(500));
    }

    private static void assertConnectionAvailable() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String response = connection.ping();
            if (!"PONG".equals(response)) {
                throw new IllegalStateException("External Redis integration test did not receive PONG");
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Cannot connect to external Redis for integration tests; check REDIS_IT_* environment variables",
                    exception);
        }
    }

    private static String requiredEnvironment(String name) {
        return optionalEnvironment(name)
                .orElseThrow(() -> new IllegalStateException(name + " must be configured for redis-it"));
    }

    private static java.util.Optional<String> optionalEnvironment(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(value);
    }

    private static int integerEnvironment(String name, int defaultValue) {
        return optionalEnvironment(name).map(value -> parseInteger(name, value)).orElse(defaultValue);
    }

    private static int parseInteger(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(name + " must be an integer", exception);
        }
    }

    private static boolean booleanEnvironment(String name, boolean defaultValue) {
        return optionalEnvironment(name).map(Boolean::parseBoolean).orElse(defaultValue);
    }
}
