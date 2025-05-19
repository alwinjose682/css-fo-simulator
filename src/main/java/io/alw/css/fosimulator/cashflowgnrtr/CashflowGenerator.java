package io.alw.css.fosimulator.cashflowgnrtr;

import io.alw.css.domain.cashflow.FoCashMessage;
import io.alw.css.fosimulator.CashMessagePublisher;
import io.alw.css.fosimulator.model.GeneratorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

public final class CashflowGenerator extends Stoppable implements Runnable {
    /*
    Amendment Notes:
        Trade and Cashflow amendment
        Let amendments happen on T and T+1 days.
     */

    private final static Logger log = LoggerFactory.getLogger(CashflowGenerator.class);
    private final Supplier<List<FoCashMessage>> cashMessageSupplier;
    private final Consumer<List<FoCashMessage>> cashMessageConsumer;
    private final GeneratorDetail generatorDetail;
    private final long pauseIntervalSeconds;
    private final RandomGenerator rndm;

    public CashflowGenerator(GeneratorDetail generatorDetail, Supplier<List<FoCashMessage>> cashMessageSupplier, CashMessagePublisher cashMessageConsumer) {
        super();
        this.generatorDetail = generatorDetail;
        this.cashMessageSupplier = cashMessageSupplier;
        this.pauseIntervalSeconds = generatorDetail.generationFrequency() * 1_000;
        this.rndm = RandomGenerator.getDefault();
        this.cashMessageConsumer = cashMessageConsumer;
    }

    @Override
    public void run() {
        try {
            // Before actual start, pauses the generator for a random small amount of time so that all the generators do not appear to start at the same time when checking the logs
            long pauseTimeBeforeActualStart = rndm.nextLong(0, pauseIntervalSeconds);
            Thread.sleep(pauseTimeBeforeActualStart);
            // Start
            while (!isStopSignalled()) {
                List<FoCashMessage> foCashMessages = cashMessageSupplier.get();
                cashMessageConsumer.accept(foCashMessages);
                Thread.sleep(pauseIntervalSeconds);
            }
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // TODO and remove below throw of runtimeException
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Exception occurred for generator: {}, generationFrequency: {}. Exception: {}", generatorDetail.generatorKey(), generatorDetail.generationFrequency(), e.getMessage(), e);
            e.printStackTrace();
        } finally {
            markTaskExecutionIsCompleted();
        }
    }

    @Override
    protected void markTaskExecutionIsCompleted() {
        setTaskExecutionAsCompleted();
    }
}
