/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Constant-cardinality counters shared by all page channels of an application.
 */
final class OperationMetrics {
    /**
     * Requests waiting for the root monitor.
     */
    private final AtomicLong waiting = new AtomicLong();
    /**
     * Requests holding the root monitor.
     */
    private final AtomicLong active = new AtomicLong();
    /**
     * Completed root operations, including rejected operations.
     */
    private final LongAdder completed = new LongAdder();
    /**
     * Cumulative root monitor wait.
     */
    private final LongAdder waitNanos = new LongAdder();
    /**
     * Maximum observed root monitor wait.
     */
    private final AtomicLong maxWaitNanos = new AtomicLong();

    /**
     * @return monotonic time at entry to the root-lock queue
     */
    long waiting() {
        this.waiting.incrementAndGet();
        return System.nanoTime();
    }

    /**
     * @param started entry to the root-lock queue
     */
    void acquired(final long started) {
        final long elapsed = System.nanoTime() - started;
        this.waitNanos.add(elapsed);
        this.maxWaitNanos.accumulateAndGet(elapsed, Math::max);
        this.active.incrementAndGet();
        this.waiting.decrementAndGet();
    }

    /**
     * Completes an operation even when application code throws.
     */
    void completed() {
        this.completed.increment();
        this.active.decrementAndGet();
    }

    /**
     * @return independent, immutable, numeric-only snapshot
     */
    Map<String, Long> snapshot() {
        return Map.of("rootWaiters", this.waiting.get(), "activeOperations", this.active.get(),
            "completedOperations", this.completed.sum(), "rootLockWaitNanos", this.waitNanos.sum(),
            "rootLockMaxWaitNanos", this.maxWaitNanos.get());
    }
}
