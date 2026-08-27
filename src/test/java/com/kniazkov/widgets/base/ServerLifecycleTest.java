/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reproduces lifecycle leaks in the application and server entry points.
 */
public class ServerLifecycleTest {
    /**
     * Constructing an application must not create a thread that prevents JVM shutdown.
     */
    @Test
    public void applicationWatchdogDoesNotKeepTheJvmAlive() throws Exception {
        final Set<Long> timerThreadsBefore = timerThreadIds();

        BaseTestSupport.application((root, context) -> { });
        Thread.sleep(50);

        final Set<Long> newTimerThreads = timerThreadIds();
        newTimerThreads.removeAll(timerThreadsBefore);
        assertTrue("Application created no watchdog thread", !newTimerThreads.isEmpty());
        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (newTimerThreads.contains(thread.threadId())) {
                assertTrue(
                    "The watchdog is non-daemon and has no application shutdown API",
                    thread.isDaemon()
                );
            }
        }
    }

    /**
     * Starting a server must return a handle through which it can later be stopped.
     */
    @Test
    public void serverStartReturnsAStoppableHandle() throws Exception {
        final Method start = Server.class.getMethod(
            "start", Application.class, Options.class
        );

        assertFalse(
            "Server.start discards the running web server, so callers cannot stop it",
            Void.TYPE.equals(start.getReturnType())
        );
    }

    /**
     * Returns IDs of live java.util.Timer worker threads.
     */
    private static Set<Long> timerThreadIds() {
        final Set<Long> result = new HashSet<>();
        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith("Timer-")) {
                result.add(thread.threadId());
            }
        }
        return result;
    }
}
