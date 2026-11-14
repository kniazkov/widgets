/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.model.DefaultModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
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

        final Application application = BaseTestSupport.application((root, context) -> { });
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
        application.close();
        awaitNoTimers(newTimerThreads);
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
     * Repeated starts/stops purge timers, page state and even a detached widget listener.
     */
    @Test
    public void repeatedStopReleasesOwnedResourcesAndAllowsSamePortRestart() throws Exception {
        final InetAddress local = InetAddress.getLoopbackAddress();
        final Set<Long> previous = timerThreadIds();
        final ObservedText shared = new ObservedText();
        final AtomicInteger cleanup = new AtomicInteger();
        int port = 0;
        for (int iteration = 0; iteration < 3; iteration++) {
            final Application app = new Application((root, context) -> {
                final TextWidget text = new TextWidget("initial");
                text.setTextModel(shared);
                final Section parent = new Section(text);
                root.add(parent);
                text.remove();
                root.onClose(cleanup::incrementAndGet);
                root.onClose(() -> {
                    throw new IllegalStateException("expected fixture failure");
                });
            });
            app.onClose(cleanup::incrementAndGet);
            final var server = Server.start(app, new Options.Builder().setPort(port)
                .setBindAddress(local).build());
            try {
                port = server.getPort();
                final var id = app.createClient("/", new PageContext());
                assertTrue(shared.listeners.get() > 0);
                if (iteration == 1) {
                    app.close();
                } else {
                    server.stop();
                }
                app.close();
                server.stop();
                assertTrue(app.isClosed());
                assertEquals(0, shared.listeners.get());
                assertEquals((iteration + 1) * 2, cleanup.get());
                assertTrue(BaseTestSupport.clients(app).isEmpty());
                final JsonObject response = new JsonObject();
                app.synchronize(id, Map.of(), response);
                assertFalse(response.containsKey("clientAlive"));
                assertThrows(ProtocolAccessException.class,
                    () -> app.createClient("/", new PageContext()));
            } finally {
                server.stop();
            }
        }
        final Set<Long> timers = timerThreadIds();
        timers.removeAll(previous);
        awaitNoTimers(timers);
    }

    /**
     * A bind collision closes registrations without touching the existing listener.
     */
    @Test
    public void bindFailureClosesTheApplication() throws Exception {
        final AtomicInteger cleanup = new AtomicInteger();
        final var app = new Application((root, context) -> { });
        app.onClose(cleanup::incrementAndGet);
        final InetAddress local = InetAddress.getLoopbackAddress();
        try (var occupied = new ServerSocket(0, 1, local)) {
            assertThrows(IllegalStateException.class, () -> Server.start(app,
                new Options.Builder().setPort(occupied.getLocalPort()).setBindAddress(local)
                    .build()));
            assertFalse(occupied.isClosed());
        }
        assertTrue(app.isClosed());
        assertEquals(1, cleanup.get());
        assertThrows(IllegalStateException.class, () -> app.onClose(() -> { }));
    }

    /**
     * Page construction racing shutdown must not reopen the client registry.
     */
    @Test
    public void closeDuringPageConstructionDiscardsPendingRegistration() throws Exception {
        final var entered = new CountDownLatch(1);
        final var resume = new CountDownLatch(1);
        final var cleanup = new AtomicInteger();
        final var app = BaseTestSupport.application((root, context) -> {
            root.onClose(cleanup::incrementAndGet);
            entered.countDown();
            try {
                assertTrue(resume.await(5, TimeUnit.SECONDS));
            } catch (final InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
        });
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var creation = executor.submit(() -> assertThrows(ProtocolAccessException.class,
                () -> app.createClient("/", new PageContext())));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                app.close();
            } finally {
                resume.countDown();
            }
            creation.get(5, TimeUnit.SECONDS);
        }
        assertTrue(BaseTestSupport.clients(app).isEmpty());
        assertEquals(1, cleanup.get());
    }

    /**
     * Drains a running synchronization before cleanup and rejects another operation.
     */
    @Test
    public void closeWhileDispatchIsInFlightDoesNotRunTheNextEvent() throws Exception {
        final var entered = new CountDownLatch(1);
        final var resume = new CountDownLatch(1);
        final var releases = new AtomicInteger();
        final var calls = new AtomicInteger();
        final Button target = new Button("Run");
        target.onClick(event -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                assertTrue(resume.await(5, TimeUnit.SECONDS));
            } catch (final InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
        });
        final var app = BaseTestSupport.application((root, context) -> {
            root.add(new Section(target));
            root.onClose(releases::incrementAndGet);
        });
        final var id = app.createClient("/", new PageContext());
        final TabContext tab = BaseTestSupport.clients(app).get(id).getTabContext();
        final JsonArray events = new JsonArray();
        for (int index = 1; index <= 2; index++) {
            final JsonObject event = new JsonObject();
            event.addString("id", "#" + index);
            event.addString("widget", target.getId().toString());
            event.addString("type", "click");
            event.put("data", EventTestData.pointer());
            events.add(event);
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var pending = executor.submit(() -> app.synchronize(id,
                Map.of("events", events.toString()), new JsonObject()));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                final var stopping = executor.submit(app::close);
                final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (tab.isActive() && System.nanoTime() < end) {
                    Thread.sleep(10);
                }
                assertTrue(app.isClosed());
                assertFalse(tab.isActive());
                assertEquals(0, releases.get());
                resume.countDown();
                stopping.get(5, TimeUnit.SECONDS);
                pending.get(5, TimeUnit.SECONDS);
            } finally {
                resume.countDown();
            }
        }
        assertEquals(1, calls.get());
        assertEquals(1, releases.get());
    }

    /**
     * Allows a cancelled Timer thread to leave its scheduling loop.
     *
     * @param timers watchdog worker IDs
     * @throws InterruptedException if the test thread is interrupted
     */
    private static void awaitNoTimers(final Set<Long> timers) throws InterruptedException {
        final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (timerThreadIds().stream().anyMatch(timers::contains)
                && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertFalse(timerThreadIds().stream().anyMatch(timers::contains));
    }

    /**
     * Observes actual subscriptions and detachments, independently of GC.
     */
    private static final class ObservedText extends DefaultModel<String> {
        /**
         * Listener registration balance.
         */
        private final AtomicInteger listeners = new AtomicInteger();

        @Override
        public void addListener(final Listener<String> listener) {
            super.addListener(listener);
            this.listeners.incrementAndGet();
        }

        @Override
        public void removeListener(final Listener<String> listener) {
            super.removeListener(listener);
            this.listeners.decrementAndGet();
        }

        @Override
        public String getDefaultData() {
            return "shared";
        }

        @Override
        public Model<String> deriveWithData(final String data) {
            return new ObservedText();
        }
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
