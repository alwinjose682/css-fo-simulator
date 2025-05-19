package io.alw.css.fosimulator.cashflowgnrtr;

sealed abstract class Stoppable permits CashflowGenerator, DayTicker {

    private volatile boolean stopSignalled;

    /// When true, it means that [Stoppable#stop()] was invoked and that there are no pending tasks for the thread and the thread will be stopped
    private volatile boolean taskExecutionCompleted;

    /// This method exists only to explicitly remind implementing classes to invoke [Stoppable#setTaskExecutionAsCompleted] when about to exit [Thread#run]
    protected abstract void markTaskExecutionIsCompleted();

    protected Stoppable() {
        this.stopSignalled = false;
        this.taskExecutionCompleted = false;
    }

    /// Other threads can call this method to signal stop when the thread is able to do so
    public void stop() {
        stopSignalled = true;
    }

    /// Only this thread can call this method
    protected void setTaskExecutionAsCompleted() {
        this.taskExecutionCompleted = true;
    }

    public boolean isStopSignalled() {
        return stopSignalled;
    }

    public boolean isTaskExecutionCompleted() {
        return taskExecutionCompleted;
    }
}
