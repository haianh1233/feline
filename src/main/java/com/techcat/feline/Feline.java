package com.techcat.feline;

import com.techcat.feline.datagen.DataGeneratorService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class Feline {
    private static final int DELAYED_START_TIME_IN_SECONDS = 15;
    private static final int TASKS_COUNT = 2;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        log.info("Starting Data Generator...");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(TASKS_COUNT);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(scheduler);

        submitTasks(completionService, scheduler);
        handleTaskCompletion(completionService);
        shutdownExecutor(scheduler);
    }

    private static void submitTasks(CompletionService<Void> completionService, ScheduledExecutorService scheduler) {
        // Immediate task submission
        completionService.submit(() -> {
            try {
                new DataGeneratorService().start();
            } catch (Exception e) {
                log.error("Error in DataGeneratorService", e);
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private static void handleTaskCompletion(CompletionService<Void> completionService) throws ExecutionException, InterruptedException {
        Future<Void> completedTask = completionService.take();
        completedTask.get();
    }

    private static void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("Executor shutdown interrupted", e);
        }
    }
}