/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the reusable periodic task lifecycle.
 */
public class PeriodicTest {
    /**
     * Returning false stops subsequent ticks.
     */
    @Test
    public void falseStopsThePeriodicTask() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch first = new CountDownLatch(1);
        final Periodic periodic = new Periodic() {
            @Override
            protected boolean tick() {
                calls.incrementAndGet();
                first.countDown();
                return false;
            }
        };

        periodic.start(5);
        assertTrue(first.await(1, TimeUnit.SECONDS));
        Thread.sleep(50);
        assertEquals(1, calls.get());
        periodic.stop();
    }

    /**
     * One faulty tick must not silently destroy all future periodic work.
     */
    @Test
    public void exceptionInOneTickDoesNotStopFutureTicks() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch recovered = new CountDownLatch(1);
        final Periodic periodic = new Periodic() {
            @Override
            protected boolean tick() {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("one failed tick");
                }
                recovered.countDown();
                return false;
            }
        };

        try {
            periodic.start(5);
            assertTrue(
                "The Timer thread died permanently after one tick threw an exception",
                recovered.await(1, TimeUnit.SECONDS)
            );
        } finally {
            periodic.stop();
        }
    }
}
