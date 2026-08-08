package io.github.ringotangs.springcommons.autoconfigure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Extracts safe, structured validation errors from Spring MVC exceptions. */
final class ValidationErrorExtractor {

    private static final String FALLBACK_CODE = "Validation";

    private final MessageSource messageSource;

    ValidationErrorExtractor(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
    }

    List<Map<String, String>> extract(Exception exception, Locale locale) {
        Objects.requireNonNull(exception, "exception must not be null");
        Objects.requireNonNull(locale, "locale must not be null");
        List<ValidationError> errors =
                switch (exception) {
                    case MethodArgumentNotValidException validationException ->
                        fromObjectErrors(validationException.getBindingResult().getAllErrors(), locale);
                    case HandlerMethodValidationException validationException
                    when !validationException.isForReturnValue() -> fromMethodValidation(validationException, locale);
                    default -> List.of();
                };
        return errors.stream().map(ValidationError::toProperty).toList();
    }

    private List<ValidationError> fromMethodValidation(HandlerMethodValidationException exception, Locale locale) {
        List<ValidationError> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                errors.addAll(fromObjectErrors(parameterErrors.getAllErrors(), locale));
            } else {
                String field = parameterName(result.getMethodParameter());
                result.getResolvableErrors().forEach(error -> errors.add(toValidationError(error, field, locale)));
            }
        }
        exception
                .getCrossParameterValidationResults()
                .forEach(error -> errors.add(toValidationError(error, null, locale)));
        return errors;
    }

    private List<ValidationError> fromObjectErrors(List<ObjectError> objectErrors, Locale locale) {
        return objectErrors.stream()
                .map(error -> toValidationError(
                        error, error instanceof FieldError fieldError ? fieldError.getField() : null, locale))
                .toList();
    }

    private ValidationError toValidationError(
            MessageSourceResolvable resolvable, @Nullable String field, Locale locale) {
        return new ValidationError(field, constraintCode(resolvable), messageSource.getMessage(resolvable, locale));
    }

    private String constraintCode(MessageSourceResolvable resolvable) {
        String @Nullable [] codes = resolvable.getCodes();
        if (codes == null) {
            return FALLBACK_CODE;
        }
        for (int index = codes.length - 1; index >= 0; index--) {
            if (codes[index] != null && !codes[index].isBlank()) {
                String code = codes[index];
                int qualifierIndex = code.indexOf('.');
                return qualifierIndex >= 0 ? code.substring(0, qualifierIndex) : code;
            }
        }
        return FALLBACK_CODE;
    }

    private String parameterName(MethodParameter parameter) {
        String annotationName = annotationName(parameter);
        if (annotationName != null && !annotationName.isBlank()) {
            return annotationName;
        }
        String parameterName = parameter.getParameterName();
        return parameterName != null ? parameterName : "arg" + parameter.getParameterIndex();
    }

    private @Nullable String annotationName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            return firstNonBlank(requestParam.name(), requestParam.value());
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return firstNonBlank(pathVariable.name(), pathVariable.value());
        }
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            return firstNonBlank(requestHeader.name(), requestHeader.value());
        }
        CookieValue cookieValue = parameter.getParameterAnnotation(CookieValue.class);
        if (cookieValue != null) {
            return firstNonBlank(cookieValue.name(), cookieValue.value());
        }
        MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
        if (matrixVariable != null) {
            return firstNonBlank(matrixVariable.name(), matrixVariable.value());
        }
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        if (requestPart != null) {
            return firstNonBlank(requestPart.name(), requestPart.value());
        }
        ModelAttribute modelAttribute = parameter.getParameterAnnotation(ModelAttribute.class);
        if (modelAttribute != null) {
            return firstNonBlank(modelAttribute.name(), modelAttribute.value());
        }
        return null;
    }

    private @Nullable String firstNonBlank(String first, String second) {
        if (!first.isBlank()) {
            return first;
        }
        return second.isBlank() ? null : second;
    }

    private record ValidationError(@Nullable String field, String code, String message) {

        Map<String, String> toProperty() {
            Map<String, String> property = new LinkedHashMap<>();
            if (field != null) {
                property.put("field", field);
            }
            property.put("code", code);
            property.put("message", message);
            return property;
        }
    }
}
