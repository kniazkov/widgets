/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests the event and update protocol implemented by {@link Client}. */
public class ClientProtocolTest {
    /** Events are dispatched at most once and the last event is reported to the browser. */
    @Test
    public void dispatchesAnEventOnlyOnce() {
        final Client client = new Client();
        final Button button = new Button("Run");
        final AtomicInteger calls = new AtomicInteger();
        button.onClick(event -> calls.incrementAndGet());
        client.getRootWidget().add(new Section(button));
        final RMId eventId = RMId.create();
        final String event = "[{\"id\":\"" + eventId + "\",\"widget\":\""
            + button.getId() + "\",\"type\":\"click\",\"data\":{}}]";
        final Map<String, String> request = Collections.singletonMap("events", event);
        final JsonObject first = new JsonObject();
        final JsonObject second = new JsonObject();

        client.synchronize(request, first);
        client.synchronize(request, second);

        assertEquals(1, calls.get());
        assertEquals(eventId.toString(), first.get("lastEvent").getStringValue());
        assertEquals(eventId.toString(), second.get("lastEvent").getStringValue());
    }

    /** Acknowledged protocol updates are removed from subsequent responses. */
    @Test
    public void removesAcknowledgedUpdates() {
        final Client client = new Client();
        client.getRootWidget().add(new Section(new TextWidget("hello")));
        final JsonObject first = new JsonObject();
        client.synchronize(Collections.emptyMap(), first);
        final JsonArray updates = first.get("updates").toJsonArray();
        final String lastUpdate = updates.getElement(updates.size() - 1)
            .toJsonObject().get("id").getStringValue();
        final JsonObject second = new JsonObject();

        client.synchronize(Collections.singletonMap("lastUpdate", lastUpdate), second);

        assertTrue(second.get("updates").toJsonArray().isEmpty());
    }

    /** Malformed external events must not escape the protocol boundary as runtime failures. */
    @Test
    public void malformedEventDoesNotCrashSynchronization() {
        final Client client = new Client();
        final Map<String, String> request = Collections.singletonMap(
            "events",
            "[{\"id\":\"#1\",\"type\":\"click\",\"data\":{}}]"
        );

        try {
            client.synchronize(request, new JsonObject());
        } catch (final RuntimeException error) {
            fail("Malformed browser input escaped Client.synchronize(): " + error);
        }
    }

    /** A model callback may not corrupt a widget queue while that queue is being drained. */
    @Test(timeout = 5000)
    public void backgroundModelUpdateIsSafeWhileClientCollectsUpdates() throws Exception {
        final Client client = new Client();
        final StringModel model = new StringModel("before");
        final TextWidget widget = new TextWidget(TextWidget.getDefaultStyle(), model);
        client.getRootWidget().add(new Section(widget));
        assertTrue(model.setData("queued-one"));
        assertTrue(model.setData("queued-two"));
        final Collection<Update> target = BaseTestSupport.updates(widget);
        assertTrue("The test requires at least two queued widget updates", target.size() > 1);
        final BlockingUpdateSet pending = new BlockingUpdateSet(target);
        BaseTestSupport.replaceUpdates(client, pending);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<?> synchronization = executor.submit(
            () -> client.synchronize(Collections.emptyMap(), new JsonObject())
        );

        try {
            assertTrue("Client did not start draining the widget", pending.awaitDrain());
            assertTrue(model.setData("after"));
            pending.continueDrain();
            synchronization.get(2, TimeUnit.SECONDS);
        } catch (final ExecutionException error) {
            fail("A background model update corrupted the update queue: " + error.getCause());
        } finally {
            pending.continueDrain();
            executor.shutdownNow();
        }
    }

    /** Set that pauses while the target widget's update list is being iterated. */
    private static final class BlockingUpdateSet extends TreeSet<Update> {
        private final Collection<Update> target;
        private final CountDownLatch draining = new CountDownLatch(1);
        private final CountDownLatch proceed = new CountDownLatch(1);
        private final AtomicBoolean paused = new AtomicBoolean();
        private boolean targetCall;

        BlockingUpdateSet(final Collection<Update> target) {
            this.target = target;
        }

        @Override
        public boolean addAll(final Collection<? extends Update> updates) {
            if (updates != this.target) {
                return super.addAll(updates);
            }
            this.targetCall = true;
            try {
                return super.addAll(updates);
            } finally {
                this.targetCall = false;
            }
        }

        @Override
        public boolean add(final Update update) {
            if (this.targetCall && this.paused.compareAndSet(false, true)) {
                this.draining.countDown();
                try {
                    this.proceed.await();
                } catch (final InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
            return super.add(update);
        }

        boolean awaitDrain() throws InterruptedException {
            return this.draining.await(2, TimeUnit.SECONDS);
        }

        void continueDrain() {
            this.proceed.countDown();
        }
    }
}
