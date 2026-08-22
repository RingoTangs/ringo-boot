package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class VerificationServiceTest {

    @Test
    void declaresSymmetricResultsAndUnifiedVerificationExceptions() throws NoSuchMethodException {
        Method issue = VerificationService.class.getMethod("issue", VerificationKey.class);
        Method verify = VerificationService.class.getMethod("verify", VerificationKey.class, String.class);

        assertEquals(IssueResult.class, issue.getReturnType());
        assertEquals(VerifyResult.class, verify.getReturnType());
        assertArrayEquals(new Class<?>[] {VerificationException.class}, issue.getExceptionTypes());
        assertArrayEquals(new Class<?>[] {VerificationException.class}, verify.getExceptionTypes());
        assertThrows(
                NoSuchMethodException.class,
                () -> VerificationService.class.getMethod("issue", VerificationKey.class, VerificationPolicy.class));
    }
}
