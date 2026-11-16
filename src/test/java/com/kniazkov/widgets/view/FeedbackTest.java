/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Feedback state transitions and resource bounds are independent of browser rendering.
 */
public final class FeedbackTest {
    /**
     * Progress has explicit terminal state and validates all numerical boundaries.
     */
    @Test
    public void progressBoundariesAndCompletion() {
        final Progress progress = new Progress("Loading");
        assertEquals(-1, progress.getPercentage());
        progress.setPercentage(0);
        progress.setPercentage(100);
        assertTrue(progress.isRunning());
        assertThrows(IllegalArgumentException.class, () -> progress.setPercentage(101));
        assertThrows(IllegalArgumentException.class, () -> progress.setPercentage(-2));
        progress.finish("Ready", Feedback.Severity.SUCCESS);
        assertFalse(progress.isRunning());
        assertEquals("Ready", progress.getText());
        progress.finish("Late failure", Feedback.Severity.ERROR);
        assertEquals("Ready", progress.getText());
        assertThrows(IllegalStateException.class, () -> progress.setPercentage(50));
    }

    /**
     * Full queues never evict important errors; owned dismissal releases capacity.
     */
    @Test
    public void toastCapacityAndOwnership() {
        final Toast toast = new Toast(2, "Close");
        assertTrue(toast.offer("Error", Feedback.Severity.ERROR, 1000));
        assertTrue(toast.offer("Saved", Feedback.Severity.SUCCESS, 0));
        assertFalse(toast.offer("Another", Feedback.Severity.INFO, 1000));
        assertEquals(2, toast.getCount());
        toast.dismiss("foreign");
        assertEquals(2, toast.getCount());
        toast.dismiss("1");
        toast.dismiss("1");
        assertEquals(1, toast.getCount());
        assertTrue(toast.offer("Another", Feedback.Severity.INFO, 1000));
        toast.clear();
        assertEquals(0, toast.getCount());
        toast.dispose();
        assertThrows(IllegalStateException.class,
            () -> toast.offer("Retired", Feedback.Severity.INFO, 1000));
    }

    /**
     * Invalid timeouts and capacities cannot create unbounded retained work.
     */
    @Test
    public void toastValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Toast(0, "Close"));
        assertThrows(IllegalArgumentException.class, () -> new Toast(21, "Close"));
        final Toast toast = new Toast(1, "Close");
        assertThrows(IllegalArgumentException.class,
            () -> toast.offer("Message", Feedback.Severity.INFO, -1));
        assertThrows(IllegalArgumentException.class,
            () -> toast.offer("Message", Feedback.Severity.INFO, 999));
        assertThrows(IllegalArgumentException.class,
            () -> toast.offer(" ", Feedback.Severity.INFO, 1000));
    }

    /**
     * Plain text and severity remain distinct; field errors can name an editor's error.
     */
    @Test
    public void localizedMessages() {
        final Alert alert = new Alert("<b>Не HTML</b>");
        assertEquals("<b>Не HTML</b>", alert.getText());
        alert.setSeverity(Feedback.Severity.WARNING);
        alert.setText("Предупреждение");
        assertEquals(Feedback.Severity.WARNING, alert.getSeverity());
        final InputField field = new InputField();
        final FieldError error = new FieldError("Некорректное значение");
        field.setAccessibleError(error);
        assertEquals(Feedback.Severity.ERROR, error.getSeverity());
        final Busy busy = new Busy("Ожидание");
        assertTrue(busy.isRunning());
        final EmptyState empty = new EmptyState("Нет заказов");
        empty.add(new Section(new Button("Добавить")));
        assertEquals(1, empty.getChildCount());
    }
    /**
     * A requested cancellation runs once; retirement prevents later callbacks.
     */
    @Test
    public void cancellationIsOnceOnly() {
        final Progress progress = new Progress("Loading");
        final var calls = new java.util.concurrent.atomic.AtomicInteger();
        progress.onCancel("Cancel", "Cancelled", calls::incrementAndGet);
        final Button button = (Button) ((Section) progress.getChild(0)).getChild(0);
        button.getController(com.kniazkov.widgets.controller.Event.CLICK).handleEvent(null);
        button.getController(com.kniazkov.widgets.controller.Event.CLICK).handleEvent(null);
        assertEquals(1, calls.get());
        assertFalse(progress.isRunning());
        assertEquals("Cancelled", progress.getText());
        final Progress retired = new Progress("Loading");
        retired.onCancel("Cancel", "Cancelled", calls::incrementAndGet);
        final Button old = (Button) ((Section) retired.getChild(0)).getChild(0);
        final var controller = old.getController(com.kniazkov.widgets.controller.Event.CLICK);
        retired.dispose();
        controller.handleEvent(null);
        assertEquals(1, calls.get());
        assertFalse(retired.isRunning());
    }

    /**
     * Expiry releases bounded metadata, but errors ignore a requested lifetime.
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void toastExpiryPreservesErrors() throws InterruptedException {
        final Toast toast = new Toast(2, "Close");
        toast.offer("Error", Feedback.Severity.ERROR, 1000);
        toast.offer("Saved", Feedback.Severity.SUCCESS, 1000);
        Thread.sleep(1100);
        assertEquals(1, toast.getCount());
        assertTrue(toast.contains("1"));
        assertFalse(toast.contains("2"));
        assertTrue(toast.offer("Next", Feedback.Severity.INFO, 1000));
    }

    /**
     * Dismissal validates ownership and shape before changing the bounded queue.
     */
    @Test
    public void validatesDismissalEvents() {
        final Toast toast = new Toast(2, "Close");
        toast.offer("Error", Feedback.Severity.ERROR, 0);
        final RootWidget root = new RootWidget(toast);
        WidgetSandbox.open(root);
        final var data = new com.kniazkov.json.JsonObject();
        data.addString("id", "foreign");
        toast.handleEvent("toast dismiss", data);
        assertEquals(1, toast.getCount());
        data.addString("id", "1");
        data.addString("extra", "unexpected");
        toast.handleEvent("toast dismiss", data);
        assertEquals(1, toast.getCount());
        final var valid = new com.kniazkov.json.JsonObject();
        valid.addString("id", "1");
        toast.handleEvent("toast dismiss", valid);
        assertEquals(0, toast.getCount());
        toast.dispose();
    }

}
