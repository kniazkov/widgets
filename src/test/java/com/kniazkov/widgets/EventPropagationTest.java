package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the event propagation chain.
 */
public class EventPropagationTest {
    /**
     * Testing the "click" event propagation chain.
     */
    @Test
    public void testClickEvent() {
        final boolean[] clicked = new boolean[1];
        final Button button = new Button();
        button.onClick(() -> clicked[0] = true);
        final JsonObject json = new JsonObject();
        json.addString("type", "click");
        json.addString("widgetId", button.getWidgetId().toString());
        button.handleEvent(json);
        Assert.assertTrue(clicked[0]);
    }

    /**
     * Testing the "text input" event propagation chain.
     */
    @Test
    public void testTextInputEvent() {
        final StringBuilder builder = new StringBuilder();
        final InputField input = new InputField();
        input.onTextInput(builder::append);
        final JsonObject json = new JsonObject();
        final String text = "abc";
        json.addString("type", "text input");
        json.addString("widgetId", input.getWidgetId().toString());
        json.addString("text", text);
        input.handleEvent(json);
        Assert.assertEquals(text, input.getTextModel().getData());
        Assert.assertEquals(text, input.getText());
        Assert.assertEquals(text, builder.toString());
    }
}
