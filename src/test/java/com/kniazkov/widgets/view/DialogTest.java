/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Typed decisions, asynchronous lifetime and Form integration use the same page context.
 */
public final class DialogTest {
    /**
     * Localized test labels.
     */
    private static final Dialog.Messages LABELS = new Dialog.Messages(
        "Apply", "Cancel", "Close", "Working", "Safe failure", "Invalid");

    /**
     * Each terminal route resolves once and cannot be changed by callers or repeated input.
     */
    @Test
    public void decisionsResolveExactlyOnce() {
        for (final Dialog.Reason reason : Dialog.Reason.values()) {
            final Dialog<Boolean> dialog = Dialog.confirmation("Confirm", "Question", LABELS);
            final RootWidget root = new RootWidget(dialog);
            final AtomicInteger observed = new AtomicInteger();
            final var result = dialog.result().toCompletableFuture();
            dialog.result().thenAccept(value -> observed.incrementAndGet());
            dialog.result().toCompletableFuture().complete(null);
            assertFalse(result.isDone());
            switch (reason) {
                case CONFIRMED -> dialog.confirm();
                case CANCELLED -> assertTrue(dialog.cancel());
                case CLOSED -> assertTrue(dialog.close());
            }
            assertEquals(reason, result.join().reason());
            assertEquals(reason == Dialog.Reason.CONFIRMED, result.join().value().isPresent());
            assertEquals(1, observed.get());
            assertEquals(0, root.getChildCount());
            dialog.confirm();
            assertFalse(dialog.close());
            dialog.dispose();
            assertEquals(1, observed.get());
        }
    }

    /**
     * Busy rejects double submit and all user dismissal, even with changed policy flags.
     */
    @Test
    public void busyGuardsActionsAndDismissal() {
        final CompletableFuture<String> work = new CompletableFuture<>();
        final AtomicInteger calls = new AtomicInteger();
        final Dialog<String> dialog = new Dialog<>("Confirm", LABELS, values -> {
            calls.incrementAndGet();
            return work;
        });
        dialog.setCloseOnOutsideClick(true);
        final RootWidget root = new RootWidget(dialog);
        WidgetSandbox.open(root);
        dialog.confirm();
        dialog.confirm();
        assertEquals(1, calls.get());
        assertTrue(dialog.isBusy());
        assertFalse(dialog.close());
        assertFalse(dialog.cancel());
        dialog.setCloseOnOutsideClick(true);
        dialog.setCloseOnEscape(true);
        dialog.handleEvent(Event.DISMISS, new JsonObject());
        dialog.handleEvent(Event.DISMISS_BY_ESCAPE, new JsonObject());
        assertEquals(1, root.getChildCount());
        work.complete("Saved");
        assertEquals("Saved", dialog.result().toCompletableFuture().join().value().orElseThrow());
        assertEquals(0, root.getChildCount());
    }

    /**
     * Required fields and application validation prevent work; retry captures fresh immutable data.
     */
    @Test
    public void validatesAndRetriesAfterFailure() {
        final var values = new ArrayList<Map<String, String>>();
        final var work = new ArrayList<CompletableFuture<String>>();
        final Dialog<String> dialog = new Dialog<>("Form", LABELS, snapshot -> {
            values.add(snapshot);
            final CompletableFuture<String> attempt = new CompletableFuture<>();
            work.add(attempt);
            return attempt;
        });
        final FormField field = new FormField("name", "Name");
        field.setRequired(true);
        dialog.addField(field);
        dialog.setValidator(snapshot -> snapshot.get("name").equals("reserved")
            ? new Form.Result(Map.of("name", "Reserved"), "") : Form.Result.success());
        final RootWidget root = new RootWidget(dialog);
        dialog.confirm();
        assertEquals(0, work.size());
        field.getEditor().setText("reserved");
        dialog.confirm();
        assertEquals("Reserved", field.getError());
        assertEquals(0, work.size());
        field.getEditor().setText("first");
        dialog.confirm();
        assertThrows(UnsupportedOperationException.class, () -> values.getFirst().clear());
        work.getFirst().completeExceptionally(new IllegalStateException("Private server details"));
        assertFalse(dialog.isBusy());
        assertFalse(dialog.result().toCompletableFuture().isDone());
        assertTrue(dialog.isCloseOnEscape());
        field.getEditor().setText("second");
        dialog.confirm();
        assertEquals(Map.of("name", "second"), values.getLast());
        work.getLast().complete("second");
        assertEquals("second", dialog.result().toCompletableFuture().join().value().orElseThrow());
        assertEquals(0, root.getChildCount());
    }

    /**
     * Synchronous action failures and null results allow another attempt without leaking details.
     */
    @Test
    public void synchronousFailuresAreRetryable() {
        final AtomicInteger count = new AtomicInteger();
        final Dialog<String> dialog = new Dialog<>("Form", LABELS, values -> {
            final int attempt = count.incrementAndGet();
            if (attempt == 1) {
                throw new IllegalArgumentException("Secret");
            }
            return CompletableFuture.completedFuture(attempt == 2 ? null : "Done");
        });
        final RootWidget root = new RootWidget(dialog);
        final var sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        dialog.confirm();
        assertFalse(dialog.isBusy());
        assertFalse(sandbox.drainUpdates().toString().contains("Secret"));
        dialog.confirm();
        assertFalse(dialog.isBusy());
        dialog.confirm();
        assertEquals(3, count.get());
        assertEquals(Dialog.Reason.CONFIRMED,
                dialog.result().toCompletableFuture().join().reason());
    }

    /**
     * Disposal closes the result and ignores work that refuses cooperative cancellation.
     */
    @Test
    public void disposalIgnoresLateResults() {
        final CompletableFuture<String> work = new CompletableFuture<>() {
            @Override
            public boolean cancel(final boolean interrupt) {
                return false;
            }
        };
        final Dialog<String> dialog = new Dialog<>("Form", LABELS, values -> work);
        final RootWidget root = new RootWidget(dialog);
        final var sandbox = WidgetSandbox.open(root);
        dialog.confirm();
        dialog.dispose();
        assertEquals(Dialog.Reason.CLOSED, dialog.result().toCompletableFuture().join().reason());
        sandbox.clearUpdates();
        work.complete("Too late");
        assertTrue(sandbox.drainUpdates().isEmpty());
        assertFalse(dialog.isBusy());
    }

    /**
     * Page shutdown settles nested dialogs and cancels pending application work.
     */
    @Test
    public void pageShutdownSettlesNestedResults() {
        final var work = new CompletableFuture<String>();
        final Dialog<Boolean> parent = Dialog.confirmation("Outer", "Question", LABELS);
        final Dialog<String> child = new Dialog<>("Inner", LABELS, values -> work);
        parent.add(child);
        final RootWidget root = new RootWidget(parent);
        WidgetSandbox.open(root);
        child.confirm();
        root.closePage();
        assertTrue(work.isCancelled());
        assertEquals(Dialog.Reason.CLOSED, child.result().toCompletableFuture().join().reason());
        assertEquals(Dialog.Reason.CLOSED, parent.result().toCompletableFuture().join().reason());
    }

    /**
     * Public labels must provide meaningful accessible names.
     */
    @Test
    public void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
            () -> new Dialog.Messages("", "Cancel", "Close", "Busy", "Error", "Invalid"));
        assertThrows(IllegalArgumentException.class,
            () -> Dialog.confirmation(" ", "Question", LABELS));
    }
}
