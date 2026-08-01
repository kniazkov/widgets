/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Tests the common creation contract of every concrete widget. */
public final class WidgetContractsTest {
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
}
