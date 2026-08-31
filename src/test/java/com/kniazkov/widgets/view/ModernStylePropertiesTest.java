/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.BoxShadow;
import com.kniazkov.widgets.common.BoxSizing;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.common.Outline;
import com.kniazkov.widgets.common.TimingFunction;
import com.kniazkov.widgets.common.Transition;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests typed visual properties and their protocol representation.
 */
public final class ModernStylePropertiesTest {
    /**
     * Verifies CSS serialization of the new immutable value objects.
     */
    @Test
    public void valueObjectsProduceCss() {
        assertEquals(
            "2px 4px 8px -1px rgba(0,0,0,0.5)",
            new BoxShadow(2, 4, 8, -1, new Color(0, 0, 0, 128)).getCSSCode()
        );
        assertEquals(
            "inset 0px 1px 2px 0px rgb(128,128,128)",
            new BoxShadow(0, 1, 2, 0, Color.GRAY, true).getCSSCode()
        );
        assertEquals(
            "all 180ms ease-out 20ms",
            new Transition(180, TimingFunction.EASE_OUT, 20).getCSSCode()
        );
        assertEquals("none", BoxShadow.NONE.getCSSCode());
        assertEquals("none", Transition.NONE.getCSSCode());
    }

    /**
     * Verifies widget model changes produce typed protocol updates.
     */
    @Test
    public void buttonProducesModernStyleUpdates() {
        final Button button = new Button();
        final WidgetSandbox<Button> sandbox = WidgetSandbox.open(button);
        sandbox.clearUpdates();

        button.setBoxShadow(State.FOCUSED, new BoxShadow(0, 0, 0, 3, Color.BLUE));
        button.setOutline(
            State.FOCUSED,
            new Outline(Color.BLUE, BorderStyle.SOLID, 2, 2)
        );
        button.setCursor(State.NORMAL, Cursor.POINTER);
        button.setTransition(new Transition(150, TimingFunction.EASE_OUT));
        button.setBoxSizing(BoxSizing.BORDER_BOX);

        final List<JsonObject> updates = sandbox.drainUpdates();
        final JsonObject shadow = singleUpdate(updates, "set box shadow", button);
        assertEquals("focused", shadow.get("state").getStringValue());
        assertEquals(
            "0px 0px 0px 3px rgb(0,0,255)",
            shadow.get("box shadow").getStringValue()
        );

        final JsonObject outline = singleUpdate(updates, "set outline", button);
        assertEquals("focused", outline.get("state").getStringValue());
        final JsonObject value = (JsonObject) outline.get("outline");
        assertEquals("solid", value.get("style").getStringValue());
        assertEquals("2px", value.get("width").getStringValue());
        assertEquals("2px", value.get("offset").getStringValue());

        assertEquals(
            "pointer",
            singleUpdate(updates, "set cursor", button).get("cursor").getStringValue()
        );
        assertEquals(
            "all 150ms ease-out 0ms",
            singleUpdate(updates, "set transition", button).get("transition").getStringValue()
        );
        assertEquals(
            "border-box",
            singleUpdate(updates, "set box sizing", button).get("box sizing").getStringValue()
        );
    }

    /**
     * Returns the only matching update.
     *
     * @param updates serialized updates
     * @param action expected action
     * @param widget expected widget
     * @return matching update
     */
    private static JsonObject singleUpdate(final List<JsonObject> updates, final String action,
            final Widget<?> widget) {
        final List<JsonObject> matches = WidgetSandbox.findUpdates(updates, action, widget);
        assertEquals("update count for " + action, 1, matches.size());
        return matches.get(0);
    }
}
