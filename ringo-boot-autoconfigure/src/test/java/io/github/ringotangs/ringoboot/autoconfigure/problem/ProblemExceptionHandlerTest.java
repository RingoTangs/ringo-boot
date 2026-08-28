package io.github.ringotangs.ringoboot.autoconfigure.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemType;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import java.net.URI;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.ProblemDetail;

class ProblemExceptionHandlerTest {

    private static final ProblemType PROBLEM_TYPE = () -> ProblemTypeDefinition.of(
            "urn:problem:test:not-found", "problem.test.not-found", "User not found", "User {0} does not exist", 404);

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final ProblemExceptionHandler handler =
            new ProblemExceptionHandler(new MessageSourceProblemMessageResolver(messageSource));

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void localizesTitleAndDetailWithArguments() {
        messageSource.addMessage("problem.test.not-found.title", Locale.SIMPLIFIED_CHINESE, "未找到用户");
        messageSource.addMessage("problem.test.not-found.detail", Locale.SIMPLIFIED_CHINESE, "用户 {0} 不存在");
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        ProblemDetail problem = handler.handleProblemException(ProblemException.withArguments(PROBLEM_TYPE, 42));

        assertEquals(URI.create("urn:problem:test:not-found"), problem.getType());
        assertEquals("未找到用户", problem.getTitle());
        assertEquals("用户 42 不存在", problem.getDetail());
        assertEquals(404, problem.getStatus());
    }

    @Test
    void fallsBackToDefaultMessagesWhenLocalizedKeysAreMissing() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        ProblemDetail problem = handler.handleProblemException(ProblemException.withArguments(PROBLEM_TYPE, 42));

        assertEquals("User not found", problem.getTitle());
        assertEquals("User 42 does not exist", problem.getDetail());
    }

    @Test
    void defaultResolverDoesNotUseLocalizedMessages() {
        messageSource.addMessage("problem.test.not-found.title", Locale.SIMPLIFIED_CHINESE, "未找到用户");
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        ProblemExceptionHandler defaultHandler = new ProblemExceptionHandler(new DefaultProblemMessageResolver());

        ProblemDetail problem = defaultHandler.handleProblemException(ProblemException.withArguments(PROBLEM_TYPE, 42));

        assertEquals("User not found", problem.getTitle());
        assertEquals("User 42 does not exist", problem.getDetail());
    }
}
