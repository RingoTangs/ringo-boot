package io.github.ringotangs.boot.play;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserProblemTypeTest {

    @Test
    void exposesSpringHttpStatusValues() {
        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                UserProblemType.INVALID_USER_ID.getHttpStatus()
        );
        assertEquals(
                HttpStatus.NOT_FOUND.value(),
                UserProblemType.USER_NOT_FOUND.getHttpStatus()
        );
    }
}
