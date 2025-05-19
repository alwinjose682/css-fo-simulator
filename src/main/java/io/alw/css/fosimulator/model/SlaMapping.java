package io.alw.css.fosimulator.model;

public record SlaMapping(
        String entityCode,
        String currCode,
        String counterpartyCode,
        String secondaryLedgerAccount
) {
}
