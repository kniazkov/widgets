/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.client.Focus;
import com.kniazkov.widgets.client.ScrollIntoView;
import com.kniazkov.widgets.client.SelectText;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Local actions contain declarative identities and bounded configuration only.
 */
public final class LocalActionsTest {
    /**
     * Encodes a live target identity, not a JavaScript expression or object tree.
     */
    @Test
    public void serializesLocalTargetsAndSelection() {
        final InputField target = new InputField();
        final var focus = new Focus(target).toJsonObject();
        assertEquals("focus widget", focus.get("action").stringValue());
        assertEquals(target.getId().toString(), focus.get("target").stringValue());
        assertEquals("scroll to widget", new ScrollIntoView(target).getAction());
        final var select = new SelectText(target, 1, 4).toJsonObject();
        assertEquals("select text", select.get("action").stringValue());
        assertEquals(1, select.get("start").intValue());
        assertEquals(4, select.get("end").intValue());
        assertThrows(IllegalArgumentException.class, () -> new SelectText(target, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SelectText(target, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new SelectText(target, 0, 65537));
    }

    /**
     * Debounce is explicit, bounded and inherited by multiline editors.
     */
    @Test
    public void configuresBoundedDebounceWithoutChangingTheModel() {
        final TextArea field = new TextArea("original");
        final var sandbox = WidgetSandbox.open(field);
        sandbox.clearUpdates();
        assertThrows(IllegalArgumentException.class, () -> field.setInputDebounce(-1));
        assertThrows(IllegalArgumentException.class, () -> field.setInputDebounce(2001));
        assertEquals(0, sandbox.drainUpdates().size());
        field.setInputDebounce(200);
        final var updates = WidgetSandbox.findUpdates(sandbox.drainUpdates(),
            "set input debounce", field);
        assertEquals(1, updates.size());
        assertEquals(200, updates.get(0).get("milliseconds").intValue());
        assertEquals("original", field.getText());
    }
}
