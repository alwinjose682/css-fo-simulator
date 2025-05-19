package io.alw.css.fosimulator.cashflowgnrtr;

import io.alw.css.fosimulator.model.GeneratorDetail;

import java.util.List;

public sealed interface CashflowGeneratorHandlerOutcome {
    String msg();

    record Success(String msg,
                   List<GeneratorDetail> startedGenerators) implements CashflowGeneratorHandlerOutcome {
    }

    record Failure(String msg,
                   List<String> stoppedGenerators, // Those that were successfully started, but interrupted later due to failure of a cashflow generator
                   List<String> failedGenerators) implements CashflowGeneratorHandlerOutcome {
    }

    record ConcurrentOperation(String msg) implements CashflowGeneratorHandlerOutcome {
    }

    record GenericMessage(String msg) implements CashflowGeneratorHandlerOutcome {
    }
}
