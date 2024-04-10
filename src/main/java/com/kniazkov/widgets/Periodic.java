package com.kniazkov.widgets;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that periodically performs some function.
 */
abstract class Periodic {

    /**
     * Periodic task.
     */
    private Task task;

    /**
     * Total time during which the specified function has been performed periodically.
     */
    private long totalTime = 0;

    /**
     * Payload (should be defined in inherited class).
     * @return Whether the periodic execution should continue, if {@code false}, it will terminate
     */
    protected abstract boolean tick();

    /**
     * Starts periodic execution.
     * @param period Period in ms
     */
    public void start(long period) {
        stop();
        task = new Task(this, period);
    }

    /**
     * Stops periodic execution.
     */
    public void stop() {
        if (task != null) {
            task.stop();
        }
    }

    /**
     * Returns total time during which the specified function has been performed periodically.
     * @return Time in ms
     */
    public long getTotalTime() {
        return totalTime;
    }

    /**
     * Periodic task.
     */
    private static class Task extends TimerTask  {
        /**
         * Timer.
         */
        private final Timer timer;

        /**
         * Owner.
         */
        private final Periodic owner;

        /**
         * Period.
         */
        private long period;

        /**
         * Constructor.
         * @param owner Owner (parent class)
         * @param period Period in ms
         */
        public Task(Periodic owner, long period) {
            this.owner = owner;
            this.period = period;
            this.timer = new Timer();
            this.timer.scheduleAtFixedRate(this, 0, period);
        }

        /**
         * Stops periodic execution.
         */
        public void stop() {
            timer.cancel();
            owner.task = null;
        }

        /**
         * Performs a payload.
         */
        @Override
        public void run() {
            owner.totalTime += period;
            if (!owner.tick()) {
                stop();
            }
        }
    }
}
