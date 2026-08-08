package io.github.ringotangs.ringoboot.verification;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Default orchestration of code generation and atomic verification storage. */
public final class DefaultVerificationService implements VerificationService {

    private final CodeGenerator codeGenerator;
    private final VerificationStore store;
    private final VerificationPolicy defaultPolicy;
    private final Clock clock;

    public DefaultVerificationService(CodeGenerator codeGenerator, VerificationStore store) {
        this(codeGenerator, store, VerificationPolicy.defaults(), Clock.systemUTC());
    }

    public DefaultVerificationService(
            CodeGenerator codeGenerator, VerificationStore store, VerificationPolicy defaultPolicy, Clock clock) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.defaultPolicy = Objects.requireNonNull(defaultPolicy, "defaultPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public IssueResult issue(VerificationKey key) {
        return issue(key, defaultPolicy);
    }

    @Override
    public IssueResult issue(VerificationKey key, VerificationPolicy policy) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        String code =
                Objects.requireNonNull(codeGenerator.generate(policy.length()), "generated code must not be null");
        if (code.isBlank() || code.length() != policy.length()) {
            throw new IllegalStateException("generated code must be non-blank and have length " + policy.length());
        }
        Instant issuedAt = clock.instant();
        return switch (store.store(key, code, policy, issuedAt)) {
            case VerificationStore.StoreResult.Stored stored -> new IssueResult.Issued(code, stored.expiresAt());
            case VerificationStore.StoreResult.Throttled throttled -> new IssueResult.Throttled(throttled.retryAfter());
        };
    }

    @Override
    public VerificationResult verify(VerificationKey key, String code) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(code, "code must not be null");
        return store.verifyAndConsume(key, code, clock.instant());
    }
}
