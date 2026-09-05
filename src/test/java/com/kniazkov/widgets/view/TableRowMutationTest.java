/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests insertion and removal of table rows at arbitrary positions.
 */
public final class TableRowMutationTest {
    /**
     * Verifies that a newly created row can be inserted between existing rows.
     */
    @Test
    public void insertsCreatedRowInTheMiddle() {
        final Row first = new Row();
        final Row last = new Row();
        final Table table = new Table(first, last);
        final WidgetSandbox<Table> sandbox = WidgetSandbox.open(table);
        sandbox.clearUpdates();

        final Row middle = table.insertRow(1);

        assertEquals(3, table.getChildCount());
        assertSame(first, table.getChild(0));
        assertSame(middle, table.getChild(1));
        assertSame(last, table.getChild(2));
        assertSame(table, middle.getParent().get());
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "insert child", middle
        );
        assertEquals(1, updates.size());
        assertEquals(table.getId().toString(),
            updates.get(0).get("container").getStringValue());
        assertEquals(1, updates.get(0).get("index").getIntValue());
    }

    /**
     * Verifies insertion of a supplied detached row at both boundary positions.
     */
    @Test
    public void insertsDetachedRowsAtBoundaries() {
        final Row middle = new Row();
        final Table table = new Table(middle);
        final Row first = new Row();
        final Row last = new Row();

        table.insertRow(0, first);
        table.insertRow(table.getChildCount(), last);

        assertEquals(3, table.getChildCount());
        assertSame(first, table.getChild(0));
        assertSame(middle, table.getChild(1));
        assertSame(last, table.getChild(2));
    }

    /**
     * Verifies that removal detaches and returns the selected row.
     */
    @Test
    public void removesAndReturnsRowByIndex() {
        final Row first = new Row();
        final Row middle = new Row();
        final Row last = new Row();
        final Table table = new Table(first, middle, last);
        final WidgetSandbox<Table> sandbox = WidgetSandbox.open(table);
        sandbox.clearUpdates();

        final Row removed = table.removeRow(1);

        assertSame(middle, removed);
        assertFalse(removed.getParent().isPresent());
        assertEquals(2, table.getChildCount());
        assertSame(first, table.getChild(0));
        assertSame(last, table.getChild(1));
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "remove child", middle
        ).size());
    }

    /**
     * Verifies strict bounds and ownership checks.
     */
    @Test
    public void rejectsInvalidIndicesAndAttachedRows() {
        final Table table = new Table(new Row());

        assertThrows(IndexOutOfBoundsException.class, () -> table.insertRow(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> table.insertRow(2));
        assertThrows(IndexOutOfBoundsException.class, () -> table.removeRow(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> table.removeRow(1));
        assertThrows(IllegalArgumentException.class,
            () -> table.insertRow(0, table.getChild(0)));
    }
}
