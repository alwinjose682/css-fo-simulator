package io.alw.css.fosimulator.service;

import io.alw.css.fosimulator.cashflowgnrtr.CashflowGeneratorHandler;
import io.alw.css.fosimulator.cashflowgnrtr.CashflowGeneratorHandlerOutcome;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CashflowGeneratorService {
    private final Logger log = LoggerFactory.getLogger(CashflowGeneratorService.class);
    private final CashflowGeneratorHandler cashflowGeneratorHandler;

    public CashflowGeneratorService(CashflowGeneratorHandler cashflowGeneratorHandler) {
        this.cashflowGeneratorHandler = cashflowGeneratorHandler;
    }

    public CashflowGeneratorHandlerOutcome startDataGeneration() {
        CashflowGeneratorHandlerOutcome outcome = cashflowGeneratorHandler.startAllGenerators();
        switch (outcome) {
            case CashflowGeneratorHandlerOutcome.ConcurrentOperation _, CashflowGeneratorHandlerOutcome.GenericMessage _ -> log.info(outcome.msg());
            case CashflowGeneratorHandlerOutcome.Failure failure -> {
                String failedGenerators = Strings.join(failure.failedGenerators(), '|') + System.lineSeparator();
                String stoppedGenerators = Strings.join(failure.stoppedGenerators(), '|') + System.lineSeparator();
                log.info("{} failedGenerators: {}stoppedGenerators: {}", failure.msg() + System.lineSeparator(), failedGenerators, stoppedGenerators);
            }
            case CashflowGeneratorHandlerOutcome.Success success -> {
                String gdMsg = success.startedGenerators().stream()
                        .map(gd -> gd.generatorKey() + ", generationFrequency: " + gd.generationFrequency())
                        .collect(Collectors.joining(System.lineSeparator()));

                log.info("{}{}", success.msg() + System.lineSeparator(), gdMsg);
            }
        }
        return outcome;
    }

    public List<CashflowGeneratorHandlerOutcome> stopDataGeneration() {
        List<CashflowGeneratorHandlerOutcome> cashflowGeneratorHandlerOutcomes = cashflowGeneratorHandler.stopAllGenerators();
        String outcomeMsgs = cashflowGeneratorHandlerOutcomes.stream().map(CashflowGeneratorHandlerOutcome::msg).collect(Collectors.joining(System.lineSeparator()));
        log.info(outcomeMsgs);
        return cashflowGeneratorHandlerOutcomes;
    }

    public CashflowGeneratorHandlerOutcome startGenerator(String generatorKey) {
        return new CashflowGeneratorHandlerOutcome.Failure("Adhoc generator starting is not fully implemented yet", null, null);
    }

    public CashflowGeneratorHandlerOutcome stopGenerator(String generatorKey) {
        return new CashflowGeneratorHandlerOutcome.Failure("Adhoc generator stopping is not fully implemented yet", null, null);
    }
}
