package io.alw.css.fosimulator.model;

import io.alw.css.domain.cashflow.TradeEventAction;
import io.alw.css.domain.cashflow.TradeEventType;

public record TradeEventAndAction(TradeEventType event, TradeEventAction action) {
}
