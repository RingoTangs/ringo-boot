package io.github.ringotangs.ringoboot.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultIssueContextManagerTest {

    private static final VerificationKey KEY = new VerificationKey("account", "login", "subject");
    private static final IssueContext CONTEXT =
            IssueContext.of(KEY, VerificationChannel.EMAIL, VerificationPolicy.defaults());

    @Test
    void appliesContributorsInOrder() {
        List<String> calls = new ArrayList<>();
        IssueContextManager manager = new DefaultIssueContextManager(List.of(
                context -> {
                    calls.add("first");
                    return context.with("tenant-id", "tenant-1");
                },
                context -> {
                    calls.add(context.attribute("tenant-id").orElseThrow());
                    return context.with("tenant", "tenant-1");
                }));

        IssueContext enriched = manager.enrich(CONTEXT);

        assertEquals(List.of("first", "tenant-1"), calls);
        assertEquals("tenant-1", enriched.attribute("tenant").orElseThrow());
    }

    @Test
    void returnsOriginalContextWhenThereAreNoContributors() {
        assertSame(CONTEXT, new DefaultIssueContextManager(List.of()).enrich(CONTEXT));
    }

    @Test
    void rejectsNullInputsAndResults() {
        assertThrows(NullPointerException.class, () -> new DefaultIssueContextManager(null));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultIssueContextManager(Arrays.asList((IssueContextContributor) null)));
        assertThrows(NullPointerException.class, () -> new DefaultIssueContextManager(List.of()).enrich(null));
        assertThrows(
                NullPointerException.class,
                () -> new DefaultIssueContextManager(List.of(context -> null)).enrich(CONTEXT));
    }

    @Test
    void contributorsCannotChangeIdentityOrExistingAttributes() {
        IssueContext attributed = CONTEXT.with("tenant", "tenant-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultIssueContextManager(List.of(context -> new IssueContext(
                                new VerificationKey("account", "login", "another"),
                                context.channel(),
                                context.policy(),
                                context.attributes())))
                        .enrich(attributed));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultIssueContextManager(List.of(context -> new IssueContext(
                                context.key(), VerificationChannel.SMS, context.policy(), context.attributes())))
                        .enrich(attributed));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultIssueContextManager(List.of(context -> new IssueContext(
                                context.key(),
                                context.channel(),
                                new VerificationPolicy(
                                        4,
                                        context.policy().ttl(),
                                        context.policy().maxAttempts()),
                                context.attributes())))
                        .enrich(attributed));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultIssueContextManager(
                                List.of(context -> IssueContext.of(context.key(), context.channel(), context.policy())))
                        .enrich(attributed));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultIssueContextManager(List.of(context -> context.with("tenant", "tenant-2")))
                        .enrich(attributed));
    }
}
