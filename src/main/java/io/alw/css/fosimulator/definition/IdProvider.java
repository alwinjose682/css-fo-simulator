package io.alw.css.fosimulator.definition;

import java.util.concurrent.atomic.AtomicLong;

final class IdProvider {
    private static IdProvider instance;

    private final AtomicLong tradeIdGenerator;
    private final AtomicLong foCfIdGenerator;

    private IdProvider() {
        this.tradeIdGenerator = new AtomicLong(100L);
        this.foCfIdGenerator = new AtomicLong(10L);
    }

    static IdProvider singleton() {
        if (instance == null) {
            synchronized (IdProvider.class) {
                if (instance == null) {
                    instance = new IdProvider();
                }
            }
        }
        return instance;
    }

    long nextTradeId() {
        return tradeIdGenerator.getAndIncrement();
    }

    long nextCashflowId() {
        return foCfIdGenerator.getAndIncrement();
    }
}
