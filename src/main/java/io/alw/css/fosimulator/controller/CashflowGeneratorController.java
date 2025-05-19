package io.alw.css.fosimulator.controller;

import io.alw.css.fosimulator.cashflowgnrtr.CashflowGeneratorHandlerOutcome;
import io.alw.css.fosimulator.service.CashflowGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/cashflow/generators", produces = MediaType.APPLICATION_JSON_VALUE)
public class CashflowGeneratorController {
    private final CashflowGeneratorService cashflowGeneratorService;
    private final String ALL_GENERATORS_KEY;

    public CashflowGeneratorController(CashflowGeneratorService cashflowGeneratorService) {
        this.cashflowGeneratorService = cashflowGeneratorService;
        ALL_GENERATORS_KEY = "all";
    }

    @PostMapping(value = "start/{generatorKey}")
    public ResponseEntity<CashflowGeneratorHandlerOutcome> startDataGeneration(@PathVariable String generatorKey) {
        final CashflowGeneratorHandlerOutcome outcome;
        if (generatorKey.equalsIgnoreCase(ALL_GENERATORS_KEY)) {
            outcome = cashflowGeneratorService.startDataGeneration();
        } else {
            outcome = cashflowGeneratorService.startGenerator(generatorKey);
        }
        return new ResponseEntity<>(outcome, HttpStatus.OK);
    }

    @PostMapping(value = "stop/{generatorKey}")
    public ResponseEntity<List<CashflowGeneratorHandlerOutcome>> stopDataGeneration(@PathVariable String generatorKey) {
        final List<CashflowGeneratorHandlerOutcome> outcome;
        if (generatorKey.equalsIgnoreCase(ALL_GENERATORS_KEY)) {
            outcome = cashflowGeneratorService.stopDataGeneration();
        } else {
            outcome = List.of(cashflowGeneratorService.stopGenerator(generatorKey));
        }
        return ResponseEntity.ok(outcome);
    }
}
