package io.github.ringotangs.ringoboot.verification.autoconfigure.redis;

import io.github.ringotangs.ringoboot.verification.store.InMemoryVerificationStore;
import io.github.ringotangs.ringoboot.verification.store.VerificationStore;

class InMemoryVerificationStoreContractTest extends VerificationStoreContract {

    private final VerificationStore store = new InMemoryVerificationStore();

    @Override
    protected VerificationStore store() {
        return store;
    }
}
