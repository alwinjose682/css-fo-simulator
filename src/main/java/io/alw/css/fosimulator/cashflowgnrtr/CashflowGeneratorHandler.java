package io.alw.css.fosimulator.cashflowgnrtr;

import io.alw.css.domain.cashflow.FoCashMessage;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.cashflow.TransactionType;
import io.alw.css.fosimulator.CashMessagePublisher;
import io.alw.css.fosimulator.CssTaskExecutor;
import io.alw.css.fosimulator.definition.FxDefinition;
import io.alw.css.fosimulator.definition.TemporaryGenericDefinition;
import io.alw.css.fosimulator.model.Entity;
import io.alw.css.fosimulator.model.GeneratorDetail;
import io.alw.css.fosimulator.model.properties.CashflowGeneratorProperties;
import io.alw.css.fosimulator.model.properties.CashMessageDefinitionProperties;
import io.alw.css.fosimulator.service.RefDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class CashflowGeneratorHandler {
    private final static Logger log = LoggerFactory.getLogger(CashflowGeneratorHandler.class);
    private final static String GENERATOR_KEY_PART_SEPARATOR = "-";
    private final AtomicBoolean activeHandlerOperation;
    private final Map<String, List<CashflowGenerator>> generatorMap;

    private final CashflowGeneratorProperties cashflowGeneratorProperties;
    private final CashMessageDefinitionProperties cashMessageDefinitionProperties;
    private final CashMessagePublisher cashMessagePublisher;
    private final RefDataService refDataService;
    private final DayTicker dayTicker;
    private final CssTaskExecutor cssTaskExecutor;

    public CashflowGeneratorHandler(CashflowGeneratorProperties cashflowGeneratorProperties, CashMessageDefinitionProperties cashMessageDefinitionProperties, CashMessagePublisher cashMessagePublisher, RefDataService refDataService, DayTicker dayTicker, CssTaskExecutor cssTaskExecutor) {
        this.cashflowGeneratorProperties = cashflowGeneratorProperties;
        this.cashMessageDefinitionProperties = cashMessageDefinitionProperties;
        this.cashMessagePublisher = cashMessagePublisher;
        this.refDataService = refDataService;
        this.dayTicker = dayTicker;
        this.activeHandlerOperation = new AtomicBoolean(false);
        this.generatorMap = new ConcurrentHashMap<>();
        this.cssTaskExecutor = cssTaskExecutor;
    }

    private boolean beginHandlerOperation() {
        return activeHandlerOperation.compareAndSet(false, true);
    }

    private boolean endHandlerOperation() {
        return activeHandlerOperation.compareAndSet(true, false);
    }

    /// First, starts the day ticker. Day ticker is started only once even if this method is invoked multiple times
    /// Second, starts one generator of each kind.
    /// Additional generators need to be started explicitly
    /// Atomic boolean is used instead of just making this method synchronized because, concurrent invocations of this method that are blocked should not attempt to start the generators again
    public CashflowGeneratorHandlerOutcome startAllGenerators() {
        boolean ok = beginHandlerOperation();
        if (!ok) {
            return new CashflowGeneratorHandlerOutcome.ConcurrentOperation("Another CashflowGeneratorHandler operation is in progress");
        }

        // If already started, calling this method has no effect
        dayTicker.start();

        List<GeneratorDetail> startedGenerators = new ArrayList<>();
        List<TradeType> listOfCurrentlySupportedTradeTypes = List.of(TradeType.values());

        // Create and start generator for all combinations of each TransactionType, TradeType and Entity
        for (TransactionType transactionType : TransactionType.values()) {
            for (TradeType tradeType : listOfCurrentlySupportedTradeTypes) {
                for (Entity entity : refDataService.entities()) {
                    String key = getKey(transactionType, tradeType, entity);
                    final long generatorSleepDurationSeconds = getGeneratorSleepDurationFor(key);

                    try {
                        // Create the FoCashMessage supplier
                        Supplier<List<FoCashMessage>> cashMessageSupplier = createCashMessageSupplier(transactionType, tradeType, entity);
                        // Create the cashflowGenerator
                        GeneratorDetail generatorDetail = new GeneratorDetail(key, generatorSleepDurationSeconds);
                        CashflowGenerator cashflowGenerator = create(generatorDetail, cashMessageSupplier, cashMessagePublisher);
                        // Start the cashflowGenerator
                        cssTaskExecutor.submit(cashflowGenerator);
                        startedGenerators.add(generatorDetail);
                    } catch (Exception e) {
                        startedGenerators.stream().map(GeneratorDetail::generatorKey).forEach(this::stop);
                        dayTicker.stop();
                        return new CashflowGeneratorHandlerOutcome.Failure(e.getMessage(), startedGenerators.stream().map(GeneratorDetail::generatorKey).toList(), List.of(key));
                    }
                }
            }
        }

        // End handler operation
        endHandlerOperation(); //TODO: What to do if not ok

        return new CashflowGeneratorHandlerOutcome.Success("Successfully started all cashflow and confirmation generators", startedGenerators);
    }

    private long getGeneratorSleepDurationFor(String generatorKey) {
        Map<String, Long> cfgProps = cashflowGeneratorProperties.frequencySeconds();
        String[] gkp = generatorKey.split(GENERATOR_KEY_PART_SEPARATOR);

        for (String key : cfgProps.keySet()) {
            if (key.equalsIgnoreCase(gkp[0] + GENERATOR_KEY_PART_SEPARATOR + gkp[1])
                    || key.equalsIgnoreCase(gkp[0] + GENERATOR_KEY_PART_SEPARATOR)
                    || key.equalsIgnoreCase(GENERATOR_KEY_PART_SEPARATOR + gkp[1])) {
                return cfgProps.get(key);
            }
        }
        return cashflowGeneratorProperties.frequencySecondsDefault();
    }

    public List<CashflowGeneratorHandlerOutcome> stopAllGenerators() {
        List<CashflowGeneratorHandlerOutcome> outcome = generatorMap.keySet().stream().map(this::stop).toList();
        dayTicker.stop();
        return outcome;
    }

    /// Creates a new generator and adds to the list of the same type of generators.
    /// Concurrent Safe. Performs this computation atomically. generatorMap is ConcurrentHashMap
    /// This method does NOT change the 'begin' or 'end' handler operation state
    private CashflowGenerator create(GeneratorDetail generatorDetail, Supplier<List<FoCashMessage>> cashMessageSupplier, CashMessagePublisher cashMessagePublisher) {
        CashflowGenerator newGenerator = new CashflowGenerator(generatorDetail, cashMessageSupplier, cashMessagePublisher);
        generatorMap.compute(generatorDetail.generatorKey(), (k, v) -> {
            if (v == null) {
                List<CashflowGenerator> generators = new ArrayList<>();
                generators.add(newGenerator);
                return generators;
            } else {
                v.add(newGenerator);
                return v;
            }
        });
        return newGenerator;
    }

    private Supplier<List<FoCashMessage>> createCashMessageSupplier(TransactionType transactionType, TradeType tradeType, Entity entity) {
        return switch (tradeType) {
            case FX -> new FxDefinition(entity, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
            case PAYMENT -> new TemporaryGenericDefinition(entity, TradeType.PAYMENT, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
            case FX_NDF -> new TemporaryGenericDefinition(entity, TradeType.FX_NDF, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
            case BOND -> new TemporaryGenericDefinition(entity, TradeType.BOND, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
            case REPO -> new TemporaryGenericDefinition(entity, TradeType.REPO, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
            case OPTION -> new TemporaryGenericDefinition(entity, TradeType.OPTION, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
        };
    }

    /// 1. Signals the [CashflowGenerator] to stop in a new Thread
    /// 2. if the
    /// This method is Concurrent Safe. Performs this computation atomically. generatorMap is ConcurrentHashMap
    /// TODO: dayTicker is not stopped if all the generators are stopped in an adhoc manner
    private CashflowGeneratorHandlerOutcome stop(String key) {
        CashflowGeneratorHandlerOutcome[] outcome = new CashflowGeneratorHandlerOutcome[1];

        // Performs this computation atomically. generatorMap is ConcurrentHashMap
        generatorMap.compute(key, (_, generators) -> {
            if (generators == null) {
                outcome[0] = new CashflowGeneratorHandlerOutcome.GenericMessage("No handler with given name exists or Incorrect cashflow generator name. Key: " + key);
                return null;
            } else {
                CashflowGenerator generator = generators.removeFirst();
                performStop(generator, key);
                outcome[0] = new CashflowGeneratorHandlerOutcome.GenericMessage("Successfully signalled stop for generator: " + key + ". The generator is expected to stop shortly. Current remaining number of generators of the same type: " + generators.size());
                if (generators.isEmpty()) {
                    return null;
                } else {
                    return generators;
                }
            }
        });

        return outcome[0];
    }

    /// 1. Stops the generator and removes the generator from the collection of generators
    /// 2. Spawns a new thread and that waits for [CashflowGenerator#isTaskExecutionCompleted] to become true for `waitTimeMinutes` minutes.
    ///     - If wait period expires and the wait condition is not met, then the spawned thread logs an error message
    private void performStop(CashflowGenerator generator, String key) {
        // Signal Stop
        generator.stop();

        // Spawn a new thread that waits for the status that says there are no pending tasks for the thread and will be stopped
        cssTaskExecutor.submit(() -> {
            int waitTimeMinutes = 1;
            boolean stopped = false;
            LocalDateTime startTime = LocalDateTime.now();
            do {
                try {
                    Thread.sleep(15 * 1_000);
                } catch (InterruptedException e) { // TODO: Thread.currentThread().interrupt ?
                    throw new RuntimeException(e);
                }
                if (generator.isTaskExecutionCompleted()) {
                    stopped = true;
                    break;
                }
            } while (Duration.between(startTime, LocalDateTime.now()).toMinutes() < waitTimeMinutes);

            if (stopped) {
                log.info("Successfully stopped generator with key: {}", key);
            } else {
                log.error("The request to stop thread did not work even after 1 minute of wait time. Generator: {}", key);
            }
        });

    }

    private String getKey(TransactionType transactionType, TradeType tradeType, Entity entity) {
        return transactionType + GENERATOR_KEY_PART_SEPARATOR + tradeType + GENERATOR_KEY_PART_SEPARATOR + entity.entityCode();
    }
}
