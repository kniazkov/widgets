/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Server ordering, parent ownership and browser event validation.
 */
public final class SortableSectionTest {
    /**
     * Moves preserve widget identity, parents and model bindings.
     */
    @Test
    public void movesWithoutRecreatingChildren() {
        final TextWidget first = new TextWidget("First");
        final TextWidget second = new TextWidget("Second");
        final SortableSection section = new SortableSection(first, second);
        final WidgetSandbox<SortableSection> sandbox = WidgetSandbox.open(section);
        sandbox.clearUpdates();
        section.move(0, 1);
        assertEquals(List.of(second, first), section.getChildren());
        assertSame(section, first.getParent().orElseThrow());
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(1, updates.size());
        assertEquals("set child order", updates.get(0).get("action").getStringValue());
        assertThrows(IndexOutOfBoundsException.class, () -> section.move(0, 2));
        assertThrows(UnsupportedOperationException.class, () -> section.getChildren().clear());
        first.setText("Changed");
        assertTrue(!sandbox.drainUpdates().isEmpty());
    }

    /**
     * A valid event updates state before notifying the application; stale events are ignored.
     */
    @Test
    public void validatesVersionAndMembershipBeforeCallback() {
        final TextWidget first = new TextWidget("First");
        final TextWidget second = new TextWidget("Second");
        final SortableSection section = new SortableSection(first, second);
        final AtomicInteger calls = new AtomicInteger();
        section.onReorder(order -> {
            assertEquals(List.of(second, first), order);
            assertEquals(order, section.getChildren());
            calls.incrementAndGet();
        });
        final WidgetSandbox<SortableSection> sandbox = WidgetSandbox.open(section);
        sandbox.clearUpdates();
        sandbox.fire(Event.REORDER, move(2, first, ""));
        assertEquals(1, calls.get());
        sandbox.fire(Event.REORDER, move(2, second, ""));
        sandbox.fire(Event.REORDER, move(3, second, "missing"));
        sandbox.fire(Event.REORDER, move(3, new TextWidget(), ""));
        sandbox.fire(Event.REORDER, move(3, second, first.getId().toString()));
        assertEquals(1, calls.get());
        assertEquals(List.of(second, first), section.getChildren());
        assertEquals(5, sandbox.drainUpdates().size());
    }

    /**
     * Sorting works without a registered callback and removal invalidates old gestures.
     */
    @Test
    public void alwaysSynchronizesAndRejectsMovesAfterRemoval() {
        final TextWidget first = new TextWidget();
        final TextWidget second = new TextWidget();
        final SortableSection section = new SortableSection(first, second);
        final WidgetSandbox<SortableSection> sandbox = WidgetSandbox.open(section);
        sandbox.fire(Event.REORDER, move(2, first, ""));
        assertSame(first, section.getChild(1));
        section.remove(first);
        sandbox.fire(Event.REORDER, move(3, second, ""));
        assertEquals(List.of(second), section.getChildren());
        assertTrue(first.getParent().isEmpty());
    }

    /**
     * Attaching an existing child transfers ownership and adding it twice is harmless.
     */
    @Test
    public void transfersOwnershipAndRejectsCycles() {
        final TextWidget child = new TextWidget();
        final Section original = new Section(child);
        final SortableSection section = new SortableSection(child);
        assertEquals(0, original.getChildCount());
        section.add(child);
        assertEquals(1, section.getChildCount());
        final InlineBlock ancestor = new InlineBlock(section);
        assertThrows(IllegalArgumentException.class, () -> section.add(ancestor));
        assertThrows(NullPointerException.class, () -> section.add(null));
        section.removeAll();
        assertEquals(0, section.getChildCount());
    }

    /**
     * Validates animation settings and serializes changes for an existing browser widget.
     */
    @Test
    public void configuresAnimationDuration() {
        final SortableSection section = new SortableSection();
        final WidgetSandbox<SortableSection> sandbox = WidgetSandbox.open(section);
        sandbox.clearUpdates();
        assertEquals(250, section.getAnimationDuration());
        assertThrows(IllegalArgumentException.class, () -> section.setAnimationDuration(-1));
        assertTrue(sandbox.drainUpdates().isEmpty());
        section.setAnimationDuration(600);
        assertEquals(600, section.getAnimationDuration());
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set animation duration", section
        );
        assertEquals(1, updates.size());
        assertEquals(600, updates.get(0).get("animation duration").getIntValue());
        section.setAnimationDuration(0);
        assertEquals(0, section.getAnimationDuration());
        assertEquals(0, sandbox.drainUpdates().get(0).get("animation duration").getIntValue());
    }

    /**
     * Builds a browser move request.
     * @param revision observed revision
     * @param child moved child
     * @param before next child identifier
     * @return serialized move
     */
    private static JsonObject move(final int revision, final Widget<?> child, final String before) {
        final JsonObject data = new JsonObject();
        data.addNumber("revision", revision);
        data.addString("child", child.getId().toString());
        data.addString("before", before);
        return data;
    }
}
