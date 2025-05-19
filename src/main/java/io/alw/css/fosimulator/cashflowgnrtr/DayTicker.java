package io.alw.css.fosimulator.cashflowgnrtr;

import io.alw.css.fosimulator.VT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

// TODO 2: Is it a good idea to use virtual threads in a scheduled executor service?
//         https://stackoverflow.com/questions/76587253/how-to-use-virtual-threads-with-scheduledexecutorservice
// TODO 3: When cashflow generators are stopped, should DayTicker also be stopped ? - DONE
public final class DayTicker extends Stoppable {
    private final static Logger log = LoggerFactory.getLogger(DayTicker.class);

    private final long tickerInterval;
    private final static long firstDay = 1;
    private AtomicLong day;
    private final long intervalSecondsBeforeFirstTick;
    private final long graceSecondsForGeneratorsToStart;
    private final VT vt;
    private boolean startedDayTicker;

    public DayTicker(long tickerIntervalSeconds, long intervalSecondsBeforeFirstTick, long graceSecondsForGeneratorsToStart, VT vt) {
        if (tickerIntervalSeconds < 10) {
            throw new RuntimeException("The day 'tickerIntervalSeconds' must be atleast 10 seconds");
        }
        this.tickerInterval = tickerIntervalSeconds * 1_000;
        this.day = new AtomicLong(firstDay);
        this.intervalSecondsBeforeFirstTick = intervalSecondsBeforeFirstTick * 1_000;
        this.graceSecondsForGeneratorsToStart = graceSecondsForGeneratorsToStart * 1_000;
        this.vt = vt;
        this.startedDayTicker = false;
    }

    /// Starts the day ticker only once.
    /// If already started, calling this method has no effect
    void start() {
        synchronized (this) {
            if (!startedDayTicker) {
                vt.submit(this::startTicker);
                startedDayTicker = true;
                log.info("Started day ticker. currentDay: {}, tickerInterval: {} sec, intervalSecondsBeforeFirstTick: {} sec, graceTimeForGeneratorsToStart: {} sec", day(), tickerInterval / 1000L, intervalSecondsBeforeFirstTick / 1000L, graceSecondsForGeneratorsToStart / 1000L);
            }
        }
    }

    /// Starts the day ticker.
    /// Day ticker once started will run continuously, sleeping for the configured time interval, till stop is signalled.
    /// If day ticker is stopped, flips `startedDayTicker` to false so that day ticker can be started again
    private void startTicker() {
        // Sleep for 'intervalSecondsBeforeFirstTick' + 'graceTimeForGeneratorsToStart'. The day remains 1 during this sleep period
        try {
            Thread.sleep(intervalSecondsBeforeFirstTick + graceSecondsForGeneratorsToStart);
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // TODO and remove below throw of runtimeException
            throw new RuntimeException(e);
        }

        // Run day ticker continuously till stop is signalled
        log.info("Starting to increment the day by one. currentDay: {}, tickInterval: {} sec", day(), tickerInterval / 1000L);
        try {
            while (!isStopSignalled()) {
                day.incrementAndGet();
                log.info("Day ticked! currentDay: {}", day());
                Thread.sleep(tickerInterval);
            }
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // TODO and remove below throw of runtimeException
            throw new RuntimeException(e);
        } finally {
            synchronized (this) {
                startedDayTicker = false;
            }
            markTaskExecutionIsCompleted();
            log.info("Stopped day ticker. currentDay: {}, tickerInterval: {} sec, intervalSecondsBeforeFirstTick: {} sec, graceTimeForGeneratorsToStart: {} sec", day(), tickerInterval / 1000L, intervalSecondsBeforeFirstTick / 1000L, graceSecondsForGeneratorsToStart / 1000L);
        }
    }

    public long firstDay() {
        return firstDay;
    }

    public long day() {
        return day.get();
    }

    @Override
    protected void markTaskExecutionIsCompleted() {
        setTaskExecutionAsCompleted();
    }
}
