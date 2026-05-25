package org.dsb.fundvaluation.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RetryScheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nav-retry");
        t.setDaemon(true);
        return t;
    });

    public void schedule(Runnable task, long delayMs) {
        executor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}