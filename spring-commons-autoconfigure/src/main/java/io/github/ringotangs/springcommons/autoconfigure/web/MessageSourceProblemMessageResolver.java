package io.github.ringotangs.springcommons.autoconfigure.web;

import io.github.ringotangs.springcommons.core.ProblemDefinition;
import io.github.ringotangs.springcommons.core.ProblemException;
import io.github.ringotangs.springcommons.web.ProblemMessageResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Objects;

final class MessageSourceProblemMessageResolver implements ProblemMessageResolver {

    private static final String TITLE_SUFFIX = ".title";
    private static final String DETAIL_SUFFIX = ".detail";

    private final MessageSource messageSource;

    MessageSourceProblemMessageResolver(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(
                messageSource,
                "messageSource must not be null"
        );
    }

    @Override
    public ProblemMessages resolve(ProblemException exception) {
        ProblemDefinition definition = exception.getProblemType().getDefinition();
        Locale locale = LocaleContextHolder.getLocale();
        String title = messageSource.getMessage(
                definition.messageCode() + TITLE_SUFFIX,
                null,
                definition.title(),
                locale
        );
        String detail = messageSource.getMessage(
                definition.messageCode() + DETAIL_SUFFIX,
                exception.getDetailArguments().toArray(),
                definition.defaultDetail(),
                locale
        );
        return new ProblemMessages(
                Objects.requireNonNull(title),
                Objects.requireNonNull(detail)
        );
    }
}
