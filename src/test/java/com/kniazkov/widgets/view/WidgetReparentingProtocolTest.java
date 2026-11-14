/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.protocol.Update;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Edge cases at the ownership and outgoing protocol boundary.
 */
public class WidgetReparentingProtocolTest {
    /**
     * A previously published widget cannot enter another runtime, even after detaching.
     */
    @Test
    public void rejectsForeignPublishedWidget() {
        final RootWidget first = new RootWidget();
        final RootWidget second = new RootWidget();
        final Section child = new Section();
        first.add(child);
        first.collectBrowserUpdates(new TreeSet<Update>());
        assertThrows(IllegalArgumentException.class, () -> second.add(child));
        assertSame(first, child.getParent().orElseThrow());
        first.remove(child);
        assertThrows(IllegalArgumentException.class, () -> second.add(child));
        first.add(child);
        assertSame(first, child.getParent().orElseThrow());
    }

    /**
     * Browser attaches before source removal; event handler survives former-owner disposal.
     */
    @Test
    public void movesActivePanelChildWithoutRecreatingIt() {
        final Button button = new Button("Action");
        final AtomicInteger actions = new AtomicInteger();
        button.onClick(event -> actions.incrementAndGet());
        final Section child = new Section(button);
        final Panel original = new Panel(child);
        final Panel destination = new Panel();
        final RootWidget root = new RootWidget(original, destination);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();

        destination.add(child);
        final List<JsonObject> placement = sandbox.drainUpdates().stream()
            .filter(update -> child.getId().toString().equals(update.get("widget").stringValue()))
            .toList();
        assertEquals("append child", placement.get(0).get("action").stringValue());
        assertEquals("remove child", placement.get(1).get("action").stringValue());
        assertEquals(2, placement.size());
        original.dispose();
        assertTrue(root.collectChildren(Widget.class).contains(child));
        button.handleEvent(Event.CLICK, EventTestData.pointer());
        assertEquals(1, actions.get());
    }

    /**
     * Cycle, self and a bad table index leave both owners and queues unchanged.
     */
    @Test
    public void rejectsBeforeTouchingStorageOrUpdates() {
        final Panel inner = new Panel();
        final Panel outer = new Panel(inner);
        final Row child = new Row();
        final Table source = new Table(child);
        final Table destination = new Table();
        final RootWidget root = new RootWidget(outer, source, destination);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        assertThrows(IllegalArgumentException.class, () -> inner.add(outer));
        assertThrows(IllegalArgumentException.class, () -> outer.add(outer));
        assertThrows(IllegalArgumentException.class, () -> new Panel(outer, outer));
        assertThrows(IndexOutOfBoundsException.class, () -> destination.insertRow(1, child));
        assertSame(root, outer.getParent().orElseThrow());
        assertSame(outer, inner.getParent().orElseThrow());
        assertSame(source, child.getParent().orElseThrow());
        assertEquals(0, destination.getChildCount());
        assertTrue(sandbox.drainUpdates().isEmpty());
        destination.insertRow(0, child);
        assertEquals(0, source.getChildCount());
        assertSame(child, destination.getRow(0));
    }

    /**
     * A column is a facade, not the parent; other columns cannot remove that cell.
     */
    @Test
    public void columnAppendUsesItsNewRowAsOwner() {
        final Cell cell = new Cell();
        final Row old = new Row(cell);
        final Table table = new Table(old);
        final Column column = table.getColumn(1);
        column.add(cell);
        assertEquals(0, old.getChildCount());
        assertEquals(2, table.getChildCount());
        assertSame(table.getRow(1), cell.getParent().orElseThrow());
        assertSame(cell, table.getRow(1).getChild(1));
        table.getColumn(0).remove(cell);
        assertSame(table.getRow(1), cell.getParent().orElseThrow());
        column.remove(cell);
        assertTrue(cell.getParent().isEmpty());
    }

    /**
     * A column append validates cycles before adding an auxiliary row.
     */
    @Test
    public void rejectsColumnCycleWithoutLeavingAnEmptyRow() {
        final Table table = new Table(new Row());
        final Cell ancestor = new Cell(table);
        final Row holder = new Row(ancestor);
        final Table outer = new Table(holder);
        final RootWidget root = new RootWidget(outer);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        assertThrows(IllegalArgumentException.class, () -> table.getColumn(1).add(ancestor));
        assertSame(holder, ancestor.getParent().orElseThrow());
        assertSame(ancestor, table.getParent().orElseThrow());
        assertEquals(1, table.getChildCount());
        assertEquals(0, table.getChild(0).getChildCount());
        assertTrue(sandbox.drainUpdates().isEmpty());
    }

    /**
     * Clearing a sparse column never creates placeholder cells.
     */
    @Test
    public void clearsOnlyExistingColumnCells() {
        final Cell kept = new Cell();
        final Cell removed = new Cell();
        final Row populated = new Row(kept, removed);
        final Row sparse = new Row();
        final Table table = new Table(sparse, populated);
        final RootWidget root = new RootWidget(table);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        table.getColumn(1).removeAll();
        assertEquals(0, sparse.getChildCount());
        assertEquals(1, populated.getChildCount());
        assertSame(kept, populated.getChild(0));
        assertTrue(removed.getParent().isEmpty());
        assertEquals(1, sandbox.drainUpdates().size());
    }

    /**
     * Moving before initial export keeps creation on the moved subtree itself.
     */
    @Test
    public void survivesMovingAndRemovingSourceBeforeFirstExport() {
        final Section child = new Section();
        final Panel original = new Panel(child);
        final Panel destination = new Panel();
        final RootWidget root = new RootWidget(original, destination);
        destination.add(child);
        original.remove();
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(1, WidgetSandbox.findUpdates(updates, "create widget", child).size());
        assertEquals(1, WidgetSandbox.findUpdates(updates, "append child", child).size());
    }
}
