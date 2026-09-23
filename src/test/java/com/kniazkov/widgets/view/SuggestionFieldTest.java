/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ReadOnlyModel;
import com.kniazkov.widgets.model.StringModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests direct source-model bindings and independent editable text.
 */
public final class SuggestionFieldTest {
    /**
     * Tests separator defaults, inherited styles and live model rebinding.
     */
    @Test
    public void bindsReactiveSeparatorWithoutChangingTextOrSuggestions() {
        assertEquals("", new SuggestionField().getSuggestionSeparator());
        final SuggestionFieldStyle style = SuggestionField.getDefaultStyle().derive();
        style.setSuggestionSeparator(",");
        final SuggestionField field = new SuggestionField(style, List.of("Black", "White"));
        assertEquals(",", field.getSuggestionSeparator());
        final StringModel separator = new StringModel(";");
        field.setSuggestionSeparatorModel(separator);
        field.setText("Black, Wh");
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        sandbox.clearUpdates();
        separator.setData("|");
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestion separator", field);
        assertEquals(1, updates.size());
        assertEquals("|", updates.get(0).get("suggestion separator").getStringValue());
        assertSame(separator, field.getSuggestionSeparatorModel());
        field.setSuggestionSeparatorModel(new StringModel(""));
        sandbox.clearUpdates();
        separator.setData(",");
        assertEquals(0, sandbox.drainUpdates().size());
        assertEquals("Black, Wh", field.getText());
        assertEquals("Black", field.getSuggestionModel(0).getData());
    }

    /**
     * The constructor retains existing models rather than copying their string values.
     */
    @Test
    public void retainsModelsAndReactsToSourceChanges() {
        final StringModel source = new StringModel("Silver");
        final List<Model<String>> models = new ArrayList<>(List.of(source));
        final SuggestionField first = new SuggestionField(models);
        final SuggestionField second = new SuggestionField(models);
        assertSame(source, first.getSuggestionModel(0));
        assertSame(source, second.getSuggestionModels().get(0));
        models.clear();
        assertEquals(1, first.getSuggestionModels().size());
        assertThrows(UnsupportedOperationException.class,
            () -> first.getSuggestionModels().clear());
        final WidgetSandbox<SuggestionField> one = WidgetSandbox.open(first);
        final WidgetSandbox<SuggestionField> two = WidgetSandbox.open(second);
        one.clearUpdates();
        two.clearUpdates();
        source.setData("Gold");
        assertSuggestions(one, first, List.of("Gold"));
        assertSuggestions(two, second, List.of("Gold"));
    }

    /**
     * Rebinding detaches obsolete listeners and preserves the entered text.
     */
    @Test
    public void replacesIndividualModelsAndEntireList() {
        final StringModel original = new StringModel("Old");
        final StringModel replacement = new StringModel("Replacement");
        final SuggestionField field = new SuggestionField(List.of(original));
        field.setText("Draft");
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        sandbox.clearUpdates();
        field.setSuggestionModel(0, replacement);
        assertSame(replacement, field.getSuggestionModel(0));
        assertSuggestions(sandbox, field, List.of("Replacement"));
        original.setData("Detached");
        assertEquals(0, sandbox.drainUpdates().size());
        replacement.setData("Changed");
        assertSuggestions(sandbox, field, List.of("Changed"));
        field.setSuggestionModels(List.of(original, replacement));
        assertSuggestions(sandbox, field, List.of("Detached", "Changed"));
        field.setSuggestionModels(List.of());
        assertSuggestions(sandbox, field, List.of());
        replacement.setData("Detached too");
        assertEquals(0, sandbox.drainUpdates().size());
        assertEquals("Draft", field.getText());
    }

    /**
     * Choosing a suggestion copies text, never writes to or rebinds a source model.
     */
    @Test
    public void supportsReadOnlySourcesAndIndependentText() {
        final Model<String> source = ReadOnlyModel.create("Silver");
        final SuggestionField field = new SuggestionField(List.of(source));
        final StringModel text = new StringModel();
        field.setTextModel(text);
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        final JsonObject selection = new JsonObject();
        selection.addString("text", "Silver");
        sandbox.fire(Event.TEXT_INPUT, selection);
        assertEquals("Silver", text.getData());
        assertSame(text, field.getTextModel());
        final JsonObject edit = new JsonObject();
        edit.addString("text", "Something new");
        sandbox.fire(Event.TEXT_INPUT, edit);
        assertEquals("Something new", text.getData());
        assertEquals("Silver", source.getData());
        assertEquals(1, field.getSuggestionModels().size());
    }

    /**
     * Repeated references subscribe once and remain live until their last position is removed.
     */
    @Test
    public void handlesRepeatedModelsAndRejectsNullsBeforeRebinding() {
        final StringModel source = new StringModel("Silver");
        final SuggestionField field = new SuggestionField(List.of(source, source));
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        sandbox.clearUpdates();
        source.setData("Gold");
        assertSuggestions(sandbox, field, List.of("Gold", "Gold"));
        assertThrows(NullPointerException.class,
            () -> field.setSuggestionModels(Arrays.asList(source, null)));
        assertThrows(NullPointerException.class, () -> field.setSuggestionModels(null));
        field.setSuggestionModel(0, new StringModel("Other"));
        assertSuggestions(sandbox, field, List.of("Other", "Gold"));
        source.setData("Still bound");
        assertSuggestions(sandbox, field, List.of("Other", "Still bound"));
        assertThrows(IndexOutOfBoundsException.class, () -> field.setSuggestionModel(2, source));
    }

    /**
     * Queued and cloned initial updates keep their original values until a new update arrives.
     */
    @Test
    public void serializesImmutableSnapshots() {
        final StringModel source = new StringModel("Original");
        final SuggestionField field = new SuggestionField(List.of(source));
        source.setData("Changed");
        final WidgetSandbox<SuggestionField> sandbox = WidgetSandbox.open(field);
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestions", field);
        assertEquals(2, updates.size());
        assertEquals("Original", updates.get(0).get("suggestions").toJsonArray()
            .getElement(0).getStringValue());
        assertEquals("Changed", updates.get(1).get("suggestions").toJsonArray()
            .getElement(0).getStringValue());
    }

    /**
     * Convenience constructors still use independent models and standard input styles.
     */
    @Test
    public void convenienceConstructorsCreateStringModels() {
        final SuggestionFieldStyle style = SuggestionField.getDefaultStyle().derive();
        final SuggestionField field = new SuggestionField(style, List.of("First", "Second"));
        assertEquals("First", field.getSuggestionModel(0).getData());
        assertEquals(2, new SuggestionField("A", "B").getSuggestionModels().size());
        assertEquals(List.of(), new SuggestionField().getSuggestionModels());
        assertEquals("suggestion field", field.getType());
    }

    /**
     * Asserts exactly one full suggestion update with the expected ordered strings.
     * @param sandbox update source
     * @param field target widget
     * @param expected ordered strings
     */
    private static void assertSuggestions(final WidgetSandbox<SuggestionField> sandbox,
            final SuggestionField field, final List<String> expected) {
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set suggestions", field);
        assertEquals(1, updates.size());
        assertEquals(expected.size(), updates.get(0).get("suggestions").toJsonArray().size());
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index), updates.get(0).get("suggestions").toJsonArray()
                .getElement(index).getStringValue());
        }
    }
}
