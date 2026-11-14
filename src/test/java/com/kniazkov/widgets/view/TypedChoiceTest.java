/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Typed model identity, validation, update stream and asynchronous search lifetime.
 */
public class TypedChoiceTest {
    /**
     * Different typed values with the same visible caption.
     */
    private static final ChoiceOption<Integer> FIRST = new ChoiceOption<>("a", "Same", 10);
    /**
     * Same caption, different key and value.
     */
    private static final ChoiceOption<Integer> SECOND = new ChoiceOption<>("b", "Same", 20);
    /**
     * Visible but unavailable.
     */
    private static final ChoiceOption<Integer> DISABLED = new ChoiceOption<>("c", "Off", 30, true);

    /**
     * Browser and application model agree on stable keys independently of visual order.
     */
    @Test
    public void duplicatesReordersRemovalAndModelChanges() {
        final Select<Integer> widget = new Select<>(List.of(FIRST, SECOND, DISABLED));
        final RootWidget root = new RootWidget(new Section(widget));
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        widget.handleEvent("choose keys", payload("b"));
        assertEquals(List.of(20), widget.getSelectedValues());
        assertEquals("b", state(sandbox).get("keys").toJsonArray().orElseThrow()
            .getFirst().stringValue());

        widget.setOptions(List.of(SECOND, FIRST, DISABLED));
        assertEquals(List.of(20), widget.getSelectedValues());
        widget.setOptions(List.of(new ChoiceOption<>("b", "Renamed", 21), FIRST));
        assertEquals(List.of(21), widget.getSelectedValues());
        assertEquals("Renamed", state(sandbox).get("selectedLabel").stringValue());

        widget.getSelectionModel().setData(List.of("a"));
        assertEquals(List.of(10), widget.getSelectedValues());
        widget.setOptions(List.of(SECOND));
        assertTrue(widget.getSelectionModel().getData().isEmpty());
        assertEquals("", state(sandbox).get("selectedLabel").stringValue());
        root.closePage();
    }

    /**
     * Invalid keys neither change a model nor invoke the controller.
     */
    @Test
    public void rejectsUnavailableUnknownAndDuplicateKeys() {
        final MultiSelect<Integer> widget = new MultiSelect<>(List.of(FIRST, SECOND, DISABLED));
        final AtomicInteger callbacks = new AtomicInteger();
        widget.onChoose(keys -> callbacks.incrementAndGet());
        widget.getSelectionModel().setData(List.of("b", "a"));
        assertEquals(List.of(20, 10), widget.getSelectedValues());
        assertEquals("invalidPayload",
            Event.CHOOSE_KEYS.rejectionReason(widget, payload("c")).orElseThrow());
        widget.handleEvent("choose keys", payload("c"));
        widget.handleEvent("choose keys", payload("missing"));
        widget.handleEvent("choose keys", payload("a", "a"));
        assertEquals(0, callbacks.get());
        assertThrows(IllegalArgumentException.class,
            () -> widget.getSelectionModel().setData(List.of("c")));
        assertThrows(IllegalArgumentException.class,
            () -> widget.setOptions(List.of(FIRST, FIRST)));
        assertEquals(List.of(20, 10), widget.getSelectedValues());
        widget.handleEvent("choose keys", payload("a", "b"));
        assertEquals(1, callbacks.get());
        widget.disable();
        widget.handleEvent("choose keys", payload());
        assertEquals(1, callbacks.get());
        assertEquals(List.of(10, 20), widget.getSelectedValues());
        widget.clear();
        assertTrue(widget.getSelectionModel().getData().isEmpty());
    }

    /**
     * Slow providers may ignore cancellation; their completions must not replace newer results.
     */
    @Test
    public void cancelsAndIgnoresStaleSearchAndPaginates() {
        final ComboBox<Integer> widget = new ComboBox<>(List.of(), false);
        final ArrayList<String> calls = new ArrayList<>();
        final ArrayList<Noncancellable> futures = new ArrayList<>();
        widget.setProvider((query, cursor) -> {
            calls.add(query + ":" + cursor);
            final Noncancellable future = new Noncancellable();
            futures.add(future);
            return future;
        });
        final RootWidget root = new RootWidget(new Section(widget));
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        widget.search("query", "slow", 1);
        assertEquals("loading", state(sandbox).get("status").stringValue());
        widget.search("query", "fast", 2);
        assertEquals(1, futures.getFirst().cancels);
        futures.get(1).complete(new ComboBox.Page<>(List.of(SECOND), "next"));
        futures.getFirst().complete(new ComboBox.Page<>(List.of(FIRST), ""));
        assertEquals(List.of(SECOND), widget.getOptions());
        widget.search("more", "", 2);
        assertEquals(List.of("slow:", "fast:", "fast:next"), calls);
        futures.get(2).complete(new ComboBox.Page<>(List.of(DISABLED), ""));
        assertEquals(List.of(SECOND, DISABLED), widget.getOptions());
        widget.choose(List.of("b"));
        widget.search("query", "bad", 3);
        futures.get(3).completeExceptionally(new IllegalStateException("internal"));
        assertEquals("error", state(sandbox).get("status").stringValue());
        assertEquals(List.of(20), widget.getSelectedValues());
        widget.search("query", "none", 4);
        widget.search("cancel", "", 5);
        assertEquals(1, futures.get(4).cancels);
        futures.get(4).complete(new ComboBox.Page<>(List.of(FIRST), ""));
        assertEquals("ready", state(sandbox).get("status").stringValue());
        assertEquals(List.of(20), widget.getSelectedValues());
        root.closePage();
    }

    /**
     * Free text is separate from typed values; strict search never creates a typed value.
     */
    @Test
    public void keepsFreeTextSeparateFromStrictKeys() {
        final ComboBox<Integer> strict = new ComboBox<>(List.of(FIRST), false);
        strict.search("query", "anything", 1);
        assertEquals("", strict.getFreeText());
        assertFalse(strict.acceptsKeys(List.of("anything")));
        final ComboBox<Integer> free = new ComboBox<>(List.of(FIRST), true);
        free.search("query", "anything", 1);
        assertEquals("anything", free.getFreeText());
        assertTrue(free.getSelectedValues().isEmpty());
        free.choose(List.of("a"));
        assertEquals("", free.getFreeText());
        assertEquals(List.of(10), free.getSelectedValues());
        free.clear();
        assertTrue(free.getSelectedValues().isEmpty());
    }

    /**
     * @param keys proposed selection
     * @return normal browser event
     */
    private static JsonObject payload(final String... keys) {
        final JsonObject payload = new JsonObject();
        final var array = payload.createArray("keys");
        for (final String key : keys) {
            array.addString(key);
        }
        return payload;
    }

    /**
     * @param sandbox attached widget transport
     * @return latest choice state
     */
    private static JsonObject state(
            final WidgetSandbox<RootWidget> sandbox) {
        return sandbox.drainUpdates().stream().filter(update ->
            "set choice state".equals(update.get("action").stringValue()))
            .reduce((old, update) -> update).orElseThrow()
            .get("state").toJsonObject().orElseThrow();
    }

    /**
     * Simulates a provider whose work cannot actually stop after cancellation is requested.
     */
    private static final class Noncancellable extends CompletableFuture<ComboBox.Page<Integer>> {
        /**
         * Number of cancellation requests.
         */
        private int cancels;

        @Override
        public boolean cancel(final boolean interrupt) {
            this.cancels++;
            return false;
        }
    }
}
