package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
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
}
