/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the single-thread database dispatcher.
 */
public final class SerialDispatcherTest {
    /**
     * Verifies nested calls execute directly on the same worker thread.
     */
    @Test
    public void executesNestedCallsOnWorkerThread() {
        final SerialDispatcher dispatcher = new SerialDispatcher();
        try {
            final Thread[] threads = dispatcher.call(() -> new Thread[] {
                Thread.currentThread(),
                dispatcher.call(Thread::currentThread)
            });

            assertSame(threads[0], threads[1]);
            assertEquals("widgets-database", threads[0].getName());
        } finally {
            dispatcher.close();
        }
    }

    /**
     * Verifies concurrent callers never overlap inside dispatched actions.
     *
     * @throws Exception when a worker fails or times out
     */
    @Test
    public void serializesConcurrentCallers() throws Exception {
        final SerialDispatcher dispatcher = new SerialDispatcher();
        final ExecutorService callers = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maximum = new AtomicInteger();
        final List<Future<?>> tasks = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            tasks.add(callers.submit(() -> {
                start.await();
                dispatcher.run(() -> {
                    final int current = active.incrementAndGet();
                    maximum.accumulateAndGet(current, Math::max);
                    Thread.yield();
                    active.decrementAndGet();
                });
                return null;
            }));
        }

        start.countDown();
        try {
            for (final Future<?> task : tasks) {
                task.get(20, TimeUnit.SECONDS);
            }
            assertEquals(1, maximum.get());
        } finally {
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(5, TimeUnit.SECONDS));
            dispatcher.close();
        }
    }

    /**
     * Verifies runtime failures preserve their original identity.
     */
    @Test
    public void propagatesRuntimeFailures() {
        final SerialDispatcher dispatcher = new SerialDispatcher();
        final IllegalArgumentException expected =
            new IllegalArgumentException("failure");
        try {
            final IllegalArgumentException actual = assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.call(() -> {
                    throw expected;
                })
            );
            assertSame(expected, actual);
        } finally {
            dispatcher.close();
        }
    }

    /**
     * Verifies checked failures are wrapped for the synchronous API.
     */
    @Test
    public void wrapsCheckedFailures() {
        final SerialDispatcher dispatcher = new SerialDispatcher();
        try {
            final IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> dispatcher.call(() -> {
                    throw new Exception("checked");
                })
            );
            assertEquals("checked", error.getCause().getMessage());
        } finally {
            dispatcher.close();
        }
    }

    /**
     * Verifies a closed dispatcher rejects new work.
     */
    @Test
    public void rejectsWorkAfterClose() {
        final SerialDispatcher dispatcher = new SerialDispatcher();
        dispatcher.close();

        assertThrows(RejectedExecutionException.class,
            () -> dispatcher.run(() -> {
            }));
    }
}
