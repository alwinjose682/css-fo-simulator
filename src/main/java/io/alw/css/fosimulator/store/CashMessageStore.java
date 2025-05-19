package io.alw.css.fosimulator.store;

import io.alw.css.domain.cashflow.FoCashMessage;

import java.util.List;

public sealed interface CashMessageStore permits InMemoryCashMessageStore {
    void add(long retrievalDay, FoCashMessage foCashMessage);

    /// The list of [FoCashMessage] retrieved are removed from the store and will no longer be available again in the store
    List<FoCashMessage> remove(long retrievalDay);
}
