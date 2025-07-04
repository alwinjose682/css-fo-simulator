package io.alw.css.fosimulator;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CssTaskExecutor {
    private final ExecutorService threadPerTaskExecutorService;

    @PreDestroy
    public void shutdownExecutorService() {
        System.out.println("Shutting down the threadPerTaskExecutorService");
        threadPerTaskExecutorService.shutdownNow();
    }

    public CssTaskExecutor() {
        threadPerTaskExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Future<?> submit(Runnable task) {
        return threadPerTaskExecutorService.submit(task);
    }

    public Executor executor() {
        return threadPerTaskExecutorService;
    }
}
