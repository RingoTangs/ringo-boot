package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ringotangs.ringoboot.verification.email.EmailVerificationFacade;
import io.github.ringotangs.ringoboot.verification.sms.SmsVerificationFacade;
import org.junit.jupiter.api.Test;

class VerificationFacadeTest {

    @Test
    void channelFacadesShareTheCommonContract() {
        assertTrue(VerificationFacade.class.isAssignableFrom(EmailVerificationFacade.class));
        assertTrue(VerificationFacade.class.isAssignableFrom(SmsVerificationFacade.class));
    }
}
