package io.github.ringotangs.ringoboot.autoconfigure.verification.redis;

import io.github.ringotangs.ringoboot.verification.store.VerificationStore;
import java.security.SecureRandom;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
        store = new RedisVerificationStore(redisTemplate, secret, Duration.ofMinutes(1));
    }

    @AfterAll
    static void closeConnection() {
        connectionFactory.destroy();
    }

    @Override
    protected VerificationStore store() {
        return store;
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
