/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A base class for tasks that should be executed periodically.
 * Subclasses define the behavior in {@link #tick()}, which is called at fixed intervals.
 * If {@code tick()} returns {@code false}, the periodic execution stops automatically.
 */
public abstract class Periodic {

    private Timer timer;
    private TimerTask task;
    private long totalTime = 0;

    /**
     * Called on each timer tick.
     * @return {@code true} to continue, {@code false} to stop
     */
    protected abstract boolean tick();

    /**
     * Starts periodic execution with the given interval.
     * If already running, it is restarted.
     *
     * @param period interval in milliseconds
     */
    public synchronized void start(long period) {
        stop();
        this.totalTime = 0;
        this.timer = new Timer();
        this.task = new TimerTask() {
            @Override
            public void run() {
                totalTime += period;
                if (!tick()) {
                    stop();
                }
            }
        };
        this.timer.scheduleAtFixedRate(this.task, 0, period);
    }

    /**
     * Stops periodic execution if running.
     */
    public synchronized void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    /**
     * Returns the total time this task has been executing.
     * @return elapsed time in milliseconds
     */
    public synchronized long getTotalTime() {
        return this.totalTime;
    }
}
