package io.github.ringotangs.ringoboot.autoconfigure.problem;

import io.github.ringotangs.ringoboot.problem.ProblemException;
import io.github.ringotangs.ringoboot.problem.ProblemTypeDefinition;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 优先使用应用消息源解析文案，缺失时回退到框架内置文案。
 */
final class MessageSourceProblemMessageResolver implements ProblemMessageResolver {

    private static final String DEFAULT_MESSAGES_BASENAME =
            "io.github.ringotangs.ringoboot.autoconfigure.problem.messages";
    private static final String TITLE_SUFFIX = ".title";
    private static final String DETAIL_SUFFIX = ".detail";

    private final MessageSource messageSource;
    private final MessageSource defaultMessageSource;

    MessageSourceProblemMessageResolver(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
        ResourceBundleMessageSource defaultMessageSource = new ResourceBundleMessageSource();
        defaultMessageSource.setBasename(DEFAULT_MESSAGES_BASENAME);
        defaultMessageSource.setDefaultEncoding("UTF-8");
        defaultMessageSource.setFallbackToSystemLocale(false);
        this.defaultMessageSource = defaultMessageSource;
    }

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemTypeDefinition definition = exception.getProblemType().getDefinition();
        Locale locale = LocaleContextHolder.getLocale();
        String title = resolveMessage(definition.messageCode() + TITLE_SUFFIX, null, definition.title(), locale);
        String detail = resolveMessage(
                definition.messageCode() + DETAIL_SUFFIX,
                exception.getDetailArguments().toArray(),
                definition.defaultDetail(),
                locale);
        return new ProblemMessages(Objects.requireNonNull(title), Objects.requireNonNull(detail));
    }

    private String resolveMessage(String code, Object @Nullable [] arguments, String defaultMessage, Locale locale) {
        String applicationMessage = messageSource.getMessage(code, arguments, null, locale);
        return applicationMessage != null
                ? applicationMessage
                : Objects.requireNonNull(defaultMessageSource.getMessage(code, arguments, defaultMessage, locale));
    }
}
