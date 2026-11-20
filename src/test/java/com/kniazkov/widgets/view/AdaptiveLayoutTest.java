/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import java.util.List;
import org.junit.Test;

/**
 * Adaptive settings and composition at the Java-to-browser boundary.
 */
public class AdaptiveLayoutTest {
    /**
     * Writes direction, alignment and gap without moving or replacing children.
     */
    @Test
    public void serializesStackChangesWithoutRecreatingChildren() {
        final Stack stack = new Stack();
        final Panel first = new Panel();
        final Panel second = new Panel();
        stack.add(first);
        stack.add(second);
        final RootWidget root = new RootWidget(stack);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();

        stack.setDirection(Stack.Direction.HORIZONTAL);
        stack.setAlignment(Stack.Alignment.CENTER);
        stack.setGap(7);
        stack.setWrap(false);
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals("horizontal", property(updates, "stack direction").stringValue());
        assertEquals("center", property(updates, "stack alignment").stringValue());
        assertEquals(7, property(updates, "layout gap").intValue());
        assertTrue(!property(updates, "stack wrap").booleanValue());
        assertTrue(updates.stream().noneMatch(update ->
            update.get("action").stringValue().endsWith(" child")));
        assertSame(first, stack.getChild(0));
        assertSame(second, stack.getChild(1));
        stack.remove(first);
        assertSame(second, stack.getChild(0));
        assertEquals("remove child", sandbox.drainUpdates().getFirst().get("action").stringValue());
        root.closePage();
    }

    /**
     * Rejects invalid grid settings before touching the update stream.
     */
    @Test
    public void serializesGridTrackBoundsAndRejectsInvalidOptions() {
        final ResponsiveGrid grid = new ResponsiveGrid();
        final RootWidget root = new RootWidget(grid);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        assertThrows(IllegalArgumentException.class, () -> grid.setColumnWidths(300, 200));
        assertThrows(IllegalArgumentException.class, () -> grid.setGap(-1));
        assertTrue(sandbox.drainUpdates().isEmpty());
        grid.setColumnWidths(125, 250);
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(125, property(updates, "grid min width").intValue());
        assertEquals(250, property(updates, "grid max width").intValue());
        root.closePage();
    }

    /**
     * The adaptive Form layout still owns the R20 field used for validation.
     */
    @Test
    public void configuresFormBreakpointWithoutReplacingFieldOrEditor() {
        final FormLayout layout = new FormLayout();
        final Form form = new Form("Demo", "Submit", layout);
        final FormField field = new FormField("title", "Title");
        field.setRequired(true);
        form.addField(field);
        final RootWidget root = new RootWidget(form);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();

        layout.setBreakpoint(430);
        layout.setGap(18);
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(430, property(updates, "form breakpoint").intValue());
        assertEquals(18, property(updates, "layout gap").intValue());
        assertSame(field, layout.getChild(0));
        form.submit();
        assertEquals("This field is required.", field.getError());
        assertThrows(IllegalArgumentException.class, () -> new Form("Other", "Submit", layout));
        form.removeField(field);
        assertEquals(0, layout.getChildCount());
        root.closePage();
    }

    /**
     * Returns the latest serialized value for one property.
     * @param updates ordered updates
     * @param name property name
     * @return wrapper whose typed scalar readers delegate to the last property value
     */
    private static com.kniazkov.json.JsonElement property(final List<JsonObject> updates,
                                                          final String name) {
        return updates.stream().filter(update ->
            update.get("action").stringValue().equals("set " + name)
        ).map(update -> update.get(name)).reduce((first, second) -> second).orElseThrow();
    }
}
