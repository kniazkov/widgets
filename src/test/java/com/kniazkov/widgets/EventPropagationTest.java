package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.Map;
import java.util.TreeMap;
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
        final Map<UId, Widget> widgets = new TreeMap<>();
        final boolean[] clicked = new boolean[1];
        final Button button = new Button();
        widgets.put(button.getWidgetId(), button);
        button.onClick(() -> clicked[0] = true);
        final JsonObject json = new JsonObject();
        json.addString("type", "click");
        json.addString("widgetId", button.getWidgetId().toString());
        final Event event = Event.parse(json);
        Assert.assertNotNull(event);
        final Widget widget = widgets.get(event.getWidgetId());
        Assert.assertNotNull(widget);
        event.apply(widget);
        Assert.assertTrue(clicked[0]);
    }
}
