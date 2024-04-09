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
         * Constructor.
         * @param owner Owner (parent class)
         * @param period Period in ms
         */
        public Task(Periodic owner, long period) {
            this.owner = owner;
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
            if (!owner.tick()) {
                stop();
            }
        }
    }
}
