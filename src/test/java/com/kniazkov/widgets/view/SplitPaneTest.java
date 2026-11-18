/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.IntegerModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Percentage bounds, ownership and synchronization with application models.
 */
public class SplitPaneTest {
    /**
     * Application writes and browser commits share the same model and bounds.
     */
    @Test
    public void sharesPositionAndValidatesBrowserCommits() {
        final IntegerModel position = new IntegerModel(40);
        final SplitPane pane = new SplitPane(SplitPane.Axis.HORIZONTAL, "Catalogue", 20, 80,
            position, new Panel(), new Panel());
        final var sandbox = WidgetSandbox.open(new RootWidget(pane));
        sandbox.clearUpdates();
        position.setData(101);
        final var update = sandbox.drainUpdates().stream().filter(item ->
            "set split pane".equals(item.get("action").stringValue())).findFirst().orElseThrow();
        assertEquals(80, update.get("snapshot").objectValueOrThrow().get("position").intValue());
        assertEquals(Integer.valueOf(101), position.getData());
        pane.setPosition(-5);
        assertEquals(Integer.valueOf(20), position.getData());
        final JsonObject payload = new JsonObject();
        payload.addNumber("position", 45);
        pane.handleEvent("split resize", payload);
        assertEquals(Integer.valueOf(45), position.getData());
        payload.addNumber("position", 81);
        assertTrue(Event.SPLIT_RESIZE.rejectionReason(pane, payload).isPresent());
        pane.disable();
        payload.addNumber("position", 50);
        assertTrue(Event.SPLIT_RESIZE.rejectionReason(pane, payload).isPresent());
    }

    /**
     * Invalid construction is atomic; a third area requires removing a previous child.
     */
    @Test
    public void preservesOwnershipAndRequiresTwoAreasForResizing() {
        final Panel first = new Panel();
        final Panel second = new Panel();
        final IntegerModel value = new IntegerModel(50);
        assertThrows(IllegalArgumentException.class, () -> new SplitPane(SplitPane.Axis.VERTICAL,
            "Details", 60, 60, value, first, second));
        assertThrows(IllegalArgumentException.class, () -> new SplitPane(SplitPane.Axis.VERTICAL,
            "Details", 0, 100, value, first, first));
        final SplitPane pane = new SplitPane(SplitPane.Axis.VERTICAL, "Details", 0, 100,
            value, first, second);
        assertThrows(IllegalStateException.class, () -> pane.add(new Panel()));
        pane.remove(first);
        assertFalse(pane.acceptsPosition(50));
        pane.add(first);
        assertEquals(second, pane.getChild(0));
        assertTrue(pane.acceptsPosition(50));
        pane.dispose();
        value.setData(60);
        assertThrows(IllegalStateException.class, () -> pane.setPosition(10));
    }
}
