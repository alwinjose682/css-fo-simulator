package io.alw.css.fosimulator.store;

import io.alw.css.domain.cashflow.FoCashMessage;

import java.util.*;

/// [InMemoryCashMessageStore] is not thread safe. It is intended to be used exclusively by a single thread
public final class InMemoryCashMessageStore implements CashMessageStore {
    private final Map<Long, List<FoCashMessage>> store;

    public InMemoryCashMessageStore() {
        this.store = new HashMap<>();
    }

    @Override
    public void add(long retrievalDay, FoCashMessage foCashMessage) {
        final List<FoCashMessage> foCashMessages = store.get(retrievalDay);
        if (foCashMessages == null) {
            List<FoCashMessage> newFoCashMessageList = new ArrayList<>();
            newFoCashMessageList.add(foCashMessage);
            store.put(retrievalDay, newFoCashMessageList);
        } else {
            foCashMessages.add(foCashMessage);
        }
    }

    @Override
    public List<FoCashMessage> remove(long retrievalDay) {
        return store.remove(retrievalDay);
    }
}
