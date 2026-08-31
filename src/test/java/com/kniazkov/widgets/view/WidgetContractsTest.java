/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the common creation contract of every concrete widget.
 */
public final class WidgetContractsTest {
    /**
     * Verifies the createsEveryConcreteWidgetInIsolation behavior.
     */
    @Test
    public void createsEveryConcreteWidgetInIsolation() {
        final Object[][] cases = {
            {new RootWidget(), "root"},
            {new Panel(), "panel"},
            {new Section(), "section"},
            {new InlineBlock(), "inline block"},
            {new Table(), "table"},
            {new Row(), "row"},
            {new Cell(), "cell"},
            {new MarginDecorator(new TextWidget()), "margin decorator"},
            {new TextWidget(), "text"},
            {new ActiveText(), "active text"},
            {new ImageWidget("image.png"), "image"},
            {new ActiveImage("active.png"), "active image"},
            {new Button(), "button"},
            {new FileLoader(), "file loader"},
            {new InputField(), "input field"},
            {new PasswordInput(), "password input"},
            {new TextArea(), "text area"},
            {new CheckBox(), "checkbox"}
        };

        for (final Object[] item : cases) {
            final Widget<?> widget = (Widget<?>) item[0];
            final String expectedType = (String) item[1];
            final WidgetSandbox<?> sandbox = WidgetSandbox.open(widget);

            final List<JsonObject> creations = WidgetSandbox.findUpdates(
                sandbox.drainUpdates(), "create widget", widget
            );

            assertEquals("creation count for " + expectedType, 1, creations.size());
            assertEquals(expectedType, creations.get(0).get("type").getStringValue());
        }
    }

    /**
     * Verifies the rejectsPropertiesForUnsupportedStates behavior.
     */
    @Test
    public void rejectsPropertiesForUnsupportedStates() {
        final TextWidget widget = new TextWidget();

        try {
            widget.getColorModel(State.HOVERED);
        } catch (final IllegalArgumentException exception) {
            assertEquals("Unsupported state: hovered", exception.getMessage());
            return;
        }
        throw new AssertionError("Unsupported state was accepted");
    }

    /**
     * Verifies that only native focusable widgets expose the focused style state.
     */
    @Test
    public void focusedStateIsLimitedToFocusableWidgets() {
        final List<Widget<?>> focusable = Arrays.<Widget<?>>asList(
            new Button(),
            new FileLoader(),
            new InputField(),
            new PasswordInput(),
            new TextArea()
        );
        final List<Widget<?>> notFocusable = Arrays.<Widget<?>>asList(
            new Panel(),
            new Section(),
            new InlineBlock(),
            new Table(),
            new Row(),
            new Cell(),
            new MarginDecorator(new TextWidget()),
            new TextWidget(),
            new ActiveText(),
            new ImageWidget("image.png"),
            new ActiveImage("active.png"),
            new CheckBox()
        );

        for (final Widget<?> widget : focusable) {
            assertTrue(widget.getType(), widget.getSupportedStates().contains(State.FOCUSED));
        }
        for (final Widget<?> widget : notFocusable) {
            assertFalse(widget.getType(), widget.getSupportedStates().contains(State.FOCUSED));
        }
    }
}
