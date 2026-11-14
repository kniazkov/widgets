/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.DisposeWidget;
import com.kniazkov.widgets.protocol.RemoveChild;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Permanent retirement is separate from a reusable temporary detach.
 */
public class WidgetDisposalTest {
    /**
     * Checks detach/dispose ordering, idempotence and root ownership.
     */
    @Test
    public void emitsOneTerminalCommandAfterDetachment() {
        final RootWidget root = new RootWidget();
        final Section section = new Section(new TextWidget("Nested"));
        root.add(section);
        final TreeSet<Update> updates = new TreeSet<>();
        root.getUpdates(updates);
        updates.clear();
        section.dispose();
        section.dispose();
        root.getUpdates(updates);
        final ArrayList<Update> ordered = new ArrayList<>(updates);
        assertEquals(2, ordered.size());
        assertTrue(ordered.get(0) instanceof RemoveChild);
        assertTrue(ordered.get(1) instanceof DisposeWidget);
        final Update terminal = ordered.get(1);
        assertEquals(section.getId(), terminal.getWidgetId());
        assertTrue(section.getParent().isEmpty());
        assertEquals(0, root.getChildCount());
        assertThrows(IllegalStateException.class, root::dispose);
    }
}
