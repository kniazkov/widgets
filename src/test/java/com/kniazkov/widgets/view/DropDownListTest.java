/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests fixed reactive drop-down options and index selection rules.
 */
public final class DropDownListTest {
    /**
     * Verifies ordered text serialization and the initial no-selection state.
     */
    @Test
    public void serializesOptionsAndStartsWithoutSelection() {
        final DropDownList list = new DropDownList(List.of(
            new StringModel("Number"),
            new StringModel("Text")
        ));
        assertEquals(2, list.getOptionCount());
        assertEquals(-1, list.getSelectedIndex());
        final WidgetSandbox<DropDownList> sandbox = WidgetSandbox.open(list);
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set options", list
        );
        assertEquals(1, updates.size());
        final JsonArray options = updates.get(0).get("options").toJsonArray();
        assertEquals("Number", options.getElement(0).getStringValue());
        assertEquals("Text", options.getElement(1).getStringValue());
    }

    /**
     * Verifies reactive text changes and model replacement at a stable position.
     */
    @Test
    public void optionModelsReactAndCanBeReplaced() {
        final StringModel original = new StringModel("Old");
        final DropDownList list = new DropDownList(List.of(original));
        final WidgetSandbox<DropDownList> sandbox = WidgetSandbox.open(list);
        sandbox.clearUpdates();

        original.setData("Changed");
        assertOptionUpdate(sandbox, list, 0, "Changed");

        final StringModel replacement = new StringModel("Replacement");
        list.setOptionModel(0, replacement);
        assertSame(replacement, list.getOptionModel(0));
        assertOptionUpdate(sandbox, list, 0, "Replacement");

        original.setData("Detached");
        assertEquals(0, sandbox.drainUpdates().size());
        replacement.setData("Current");
        assertOptionUpdate(sandbox, list, 0, "Current");
        assertEquals(1, list.getOptionCount());
    }

    /**
     * Verifies that string constructors still create models but do not expose a resizable list.
     */
    @Test
    public void stringConstructorCreatesFixedModelList() {
        final DropDownList list = new DropDownList(List.of("First", "Second"));
        assertEquals("First", list.getOptionText(0));
        list.setOptionText(1, "Changed");
        assertEquals("Changed", list.getOptionText(1));
        final List<Model<String>> models = list.getOptionModels();
        assertThrows(UnsupportedOperationException.class, () -> models.add(new StringModel()));
        assertThrows(IndexOutOfBoundsException.class,
            () -> list.setOptionModel(2, new StringModel()));
    }

    /**
     * Verifies that only no-selection or an existing stable position can be selected.
     */
    @Test
    public void validatesSelectedIndex() {
        final DropDownList list = new DropDownList("First", "Second");
        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedIndex());
        list.setSelectedIndex(-1);
        assertEquals(-1, list.getSelectedIndex());
        assertThrows(IllegalArgumentException.class, () -> list.setSelectedIndex(-2));
        assertThrows(IllegalArgumentException.class, () -> list.setSelectedIndex(2));
    }

    /**
     * Extracts and verifies one reactive option update.
     *
     * @param sandbox widget sandbox
     * @param list target list
     * @param index expected option position
     * @param text expected option text
     */
    private static void assertOptionUpdate(
        final WidgetSandbox<DropDownList> sandbox,
        final DropDownList list,
        final int index,
        final String text
    ) {
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set option", list
        );
        assertEquals(1, updates.size());
        assertEquals(index, updates.get(0).get("index").getIntValue());
        assertEquals(text, updates.get(0).get("text").getStringValue());
    }
}
