package io.github.ringotangs.ringoboot.problem.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ValidationErrorExtractorTest {

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final ValidationErrorExtractor extractor = new ValidationErrorExtractor(messageSource);

    @Test
    void extractsFieldErrorsWithoutRejectedValues() throws Exception {
        FieldError fieldError = new FieldError(
                "request",
                "password",
                "secret-value",
                false,
                new String[] {"Size.request.password", "Size.password", "Size.java.lang.String", "Size"},
                null,
                "must have a valid size");
        messageSource.addMessage("Size.request.password", Locale.ENGLISH, "localized size message");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(fieldError);

        List<Map<String, String>> errors = extractor.extract(
                new MethodArgumentNotValidException(methodParameter(), bindingResult), Locale.ENGLISH);

        assertThat(errors)
                .containsExactly(Map.of("field", "password", "code", "Size", "message", "localized size message"));
        assertThat(errors.toString()).doesNotContain("secret-value");
    }

    @Test
    void omitsFieldForObjectErrors() throws Exception {
        ObjectError objectError = new ObjectError("request", new String[] {"ValidRequest"}, null, "invalid request");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(objectError);

        List<Map<String, String>> errors = extractor.extract(
                new MethodArgumentNotValidException(methodParameter(), bindingResult), Locale.ENGLISH);

        assertThat(errors).containsExactly(Map.of("code", "ValidRequest", "message", "invalid request"));
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        return new MethodParameter(ValidationErrorExtractorTest.class.getDeclaredMethod("validated", Object.class), 0);
    }

    @SuppressWarnings("unused")
    private void validated(Object request) {}
}
