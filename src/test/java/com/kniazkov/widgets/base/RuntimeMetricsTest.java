/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Numeric diagnostics must not expose identities or retain disposed pages.
 */
public final class RuntimeMetricsTest {
    /**
     * Samples queues before/after ACK and verifies immutable, stable labels.
     */
    @Test
    public void reportsQueuesAndReleasesPagesWithoutLeakingValues() {
        final Application app = BaseTestSupport.application((root, context) ->
            root.add(new Section(new TextWidget("secret-password@example.invalid"))));
        try {
            final var empty = app.getMetrics();
            final var id = app.createClient("/", new PageContext());
            final var initial = app.getMetrics();
            assertEquals(empty.keySet(), initial.keySet());
            assertEquals(1L, (long) initial.get("pages"));
            assertEquals(3L, (long) initial.get("widgets"));
            assertTrue(initial.get("bufferedUpdates") > 0);
            assertFalse(initial.toString().contains("secret"));
            assertFalse(initial.toString().contains(id.toString()));
            assertThrows(UnsupportedOperationException.class, () -> initial.put("test", 1L));
            final JsonObject response = new JsonObject();
            app.synchronize(id, Map.of("events", "[]"), response);
            assertTrue(app.getMetrics().get("pendingUpdates") > 0);
            assertEquals(0L, (long) app.getMetrics().get("bufferedUpdates"));
            app.synchronize(id, Map.of("events", "[]", "lastUpdate",
                "#" + response.get("updateThrough").intValue()), new JsonObject());
            assertEquals(0L, (long) app.getMetrics().get("pendingUpdates"));
            app.killClient(id);
            final var stopped = app.getMetrics();
            assertEquals(0L, (long) stopped.get("widgets"));
            assertEquals(0L, (long) stopped.get("pages"));
            assertEquals(2L, (long) stopped.get("completedOperations"));
        } finally {
            app.close();
        }
    }

    /**
     * Root contention is measured, and rejected requests release active-operation gauges.
     */
    @Test(timeout = 5000)
    public void recordsRootWaitAndResetsActiveCountersOnFailure() throws Exception {
        final OperationMetrics metrics = new OperationMetrics();
        final var options = new Options.Builder().build();
        final Client client = new Client(options,
            new TabContext(java.util.UUID.randomUUID(), Long.MAX_VALUE),
            new com.kniazkov.widgets.view.UploadRegistry(options), metrics);
        final CountDownLatch started = new CountDownLatch(1);
        final Thread contender;
        synchronized (client.getRootWidget()) {
            contender = Thread.ofVirtual().start(() -> {
                started.countDown();
                client.synchronize(Map.of("events", "[]"), new JsonObject());
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (metrics.snapshot().get("rootWaiters") == 0 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertEquals(1L, (long) metrics.snapshot().get("rootWaiters"));
        }
        contender.join(1000);
        assertFalse(contender.isAlive());
        client.close();
        assertThrows(ProtocolAccessException.class,
            () -> client.synchronize(Map.of(), new JsonObject()));
        assertEquals(0L, (long) metrics.snapshot().get("activeOperations"));
        assertEquals(0L, (long) metrics.snapshot().get("rootWaiters"));
        assertTrue(metrics.snapshot().get("rootLockWaitNanos") > 0);
        assertEquals(2L, (long) metrics.snapshot().get("completedOperations"));
    }
}
