/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.StringListModel;
import com.kniazkov.widgets.model.StringModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests suggestion snapshots, bindings, protocol updates and unrestricted text input.
 */
public final class SuggestionFieldTest {
    /**
     * Mutable caller lists cannot silently modify a model or queued protocol update.
     */
    @Test
    public void snapshotsProtectModelData() {
        final List<String> source = new ArrayList<>(List.of("Silver"));
        final StringListModel model = new StringListModel(source);
        source.add("Gold");
        assertEquals(List.of("Silver"), model.getData());
        assertThrows(UnsupportedOperationException.class, () -> model.getData().add("Gold"));
        model.setData(source);
        source.clear();
        assertEquals(List.of("Silver", "Gold"), model.getData());
        assertFalse(model.setData(null));
        assertFalse(model.setData(Arrays.asList("Silver", null)));
        assertFalse(model.setData(List.of("Silver", "Gold")));
        assertEquals(List.of("Other"), model.deriveWithData(List.of("Other")).getData());
    }

    /**
     * A shared list updates each field and can be rebound independently.
     */
    @Test
    public void suggestionsReactAndRebind() {
        final StringListModel history = new StringListModel(List.of("Silver", "Gold"));
        final SuggestionField first = new SuggestionField();
        final SuggestionField second = new SuggestionField();
        first.setSuggestionsModel(history);
        second.setSuggestionsModel(history);
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(first);
        final List<JsonObject> initial = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestions", first
        );
        assertEquals("Gold", initial.get(initial.size() - 1).get("suggestions").toJsonArray()
            .getElement(1).getStringValue());
        history.setData(List.of("New"));
        assertEquals(List.of("New"), second.getSuggestions());
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestions", first).size());
        final StringListModel replacement = new StringListModel();
        first.setSuggestionsModel(replacement);
        assertSame(replacement, first.getSuggestionsModel());
        sandbox.clearUpdates();
        history.setData(List.of("Detached"));
        assertEquals(0, sandbox.drainUpdates().size());
        replacement.setData(List.of("Replacement"));
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestions", first).size());
    }

    /**
     * Free text uses the existing text model and never silently modifies suggestions.
     */
    @Test
    public void acceptsNewTextWithoutGrowingHistory() {
        final SuggestionField field = new SuggestionField(List.of("Silver"));
        final StringModel text = new StringModel();
        field.setTextModel(text);
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        final JsonObject event = new JsonObject();
        event.addString("text", "Something new");
        sandbox.fire(Event.TEXT_INPUT, event);
        assertEquals("Something new", text.getData());
        assertEquals(List.of("Silver"), field.getSuggestions());
        field.setSuggestions(List.of());
        assertEquals("Something new", text.getData());
    }

    /**
     * Style inheritance and per-widget overrides use the standard property mechanism.
     */
    @Test
    public void stylesSupplySuggestionDefaults() {
        final SuggestionFieldStyle style = SuggestionField.getDefaultStyle().derive();
        style.setSuggestions(List.of("Styled"));
        final SuggestionField field = new SuggestionField(style, "Initial");
        assertEquals(List.of("Styled"), field.getSuggestions());
        field.setSuggestions(List.of("Local"));
        assertEquals(List.of("Styled"), style.getSuggestions());
        assertEquals(List.of(), new SuggestionField().getSuggestions());
        assertEquals("suggestion field", field.getType());
    }
}
