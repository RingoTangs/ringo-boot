package io.github.ringotangs.ringoboot.verification.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ringotangs.ringoboot.verification.DeliveryResult;
import io.github.ringotangs.ringoboot.verification.VerificationKey;
import io.github.ringotangs.ringoboot.verification.VerificationPolicy;
import io.github.ringotangs.ringoboot.verification.sender.CodeDelivery;
import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SmsVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void dispatchesCompleteDeliveryToSmsSender() {
        VerificationKey key = new VerificationKey("account", "login", "+8613800000000");
        AtomicReference<CodeDelivery> captured = new AtomicReference<>();
        SmsVerificationService service = new SmsVerificationService(
                length -> "123456",
                new InMemoryVerificationStore(),
                VerificationPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                captured::set);

        DeliveryResult.Delivered result = assertInstanceOf(DeliveryResult.Delivered.class, service.issue(key));

        assertEquals(key, captured.get().key());
        assertEquals("123456", captured.get().code());
        assertEquals(result.expiresAt(), captured.get().expiresAt());
    }

    @Test
    void rejectsNullSender() {
        assertThrows(
                NullPointerException.class,
                () -> new SmsVerificationService(length -> "123456", new InMemoryVerificationStore(), null));
    }
}
