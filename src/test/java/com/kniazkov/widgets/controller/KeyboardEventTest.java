/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextArea;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**

 * Tests exact subscriptions without exposing application text in a keyboard notification.

 */
public final class KeyboardEventTest {
    /**
     * Checks the bounded shape and normalization.
     */
    @Test
    public void normalizesNamedKeysAndExactModifiers() {
        final KeyboardEvent event = KeyboardEvent.parse(data("Enter", "UP", true));
        assertEquals(KeyboardEvent.Key.ENTER, event.shortcut().key());
        assertEquals(KeyboardEvent.Phase.UP, event.phase());
        assertTrue(event.shortcut().ctrl());
        assertEquals("UP:Enter:1000", event.signature());
        assertNull(KeyboardEvent.parse(data("z", "DOWN", false)));
        assertNull(KeyboardEvent.parse(data("Enter", "press", false)));
        final JsonObject extra = data("Enter", "DOWN", false);
        extra.addString("text", "local dummy input");
        assertNull(KeyboardEvent.parse(extra));
    }

    /**
     * Checks subscriptions, read-only state and retirement.
     */
    @Test
    public void requiresLiveMatchingSubscriptionAndReleasesDisposedCallbacks() {
        final InputField field = new InputField();
        final RootWidget root = new RootWidget(new Section(field));
        final AtomicInteger handled = new AtomicInteger();
        final JsonObject enter = data("Enter", "DOWN", false);
        assertEquals("inactiveShortcut",
            Event.KEYBOARD.rejectionReason(field, enter).orElseThrow());
        field.onEnter(event -> handled.incrementAndGet());
        Event.KEYBOARD.process(field, enter);
        Event.KEYBOARD.process(field, data("Enter", "DOWN", true));
        Event.KEYBOARD.process(field, data("Enter", "UP", false));
        assertEquals(1, handled.get());
        field.setReadOnly(true);
        Event.KEYBOARD.process(field, enter);
        assertEquals(1, handled.get());
        field.setReadOnly(false);
        field.disable();
        assertEquals("inactiveWidget", Event.KEYBOARD.rejectionReason(field, enter).orElseThrow());
        field.enable();
        field.onEnter(null);
        Event.KEYBOARD.process(field, enter);
        assertEquals(1, handled.get());
        field.onEnter(event -> handled.incrementAndGet());
        field.dispose();
        Event.KEYBOARD.process(field, enter);
        assertEquals(1, handled.get());
        assertThrows(IllegalStateException.class, () -> field.onEnter(Controller.stub()));
        assertEquals("unsupportedEvent", Event.KEYBOARD.rejectionReason(new Button("No key"), enter)
            .orElseThrow());
        assertTrue(root.getRootWidget().isPresent());
    }

    /**
     * Checks exact phases and multiline policy.
     */
    @Test
    public void multilineKeepsPlainEnterAndCanObserveDifferentPhases() {
        final TextArea editor = new TextArea();
        assertThrows(IllegalArgumentException.class, () -> editor.onEnter(Controller.stub()));
        final RootWidget root = new RootWidget(new Section(editor));
        final KeyboardEvent.Shortcut shortcut = new KeyboardEvent.Shortcut(KeyboardEvent.Key.ENTER,
            true, false, false, false);
        final AtomicInteger pressed = new AtomicInteger();
        final AtomicInteger released = new AtomicInteger();
        editor.onKeyDown(shortcut, true, event -> pressed.incrementAndGet());
        editor.onKeyUp(shortcut, event -> released.incrementAndGet());
        Event.KEYBOARD.process(editor, data("Enter", "DOWN", true));
        Event.KEYBOARD.process(editor, data("Enter", "UP", true));
        Event.KEYBOARD.process(editor, data("Enter", "DOWN", false));
        assertEquals(1, pressed.get());
        assertEquals(1, released.get());
        assertTrue(root.getRootWidget().isPresent());
    }

    /**
     * Builds the fixed notification for the ordinary test case.
     * @param key key name
     * @param phase key phase
     * @param ctrl Control modifier
     * @return test payload
     */
    private static JsonObject data(final String key, final String phase, final boolean ctrl) {
        final JsonObject obj = new JsonObject();
        obj.addString("key", key);
        obj.addString("phase", phase);
        obj.addBoolean("ctrl", ctrl);
        obj.addBoolean("alt", false);
        obj.addBoolean("shift", false);
        obj.addBoolean("meta", false);
        return obj;
    }
}
