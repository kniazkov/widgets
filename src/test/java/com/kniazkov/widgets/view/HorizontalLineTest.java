/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.ColorModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Separator defaults, independent styles and reactive browser updates.
 */
public final class HorizontalLineTest {
    /**
     * Verifies that thickness and color overrides do not change the default style.
     */
    @Test
    public void derivesIndependentLineStyle() {
        final HorizontalLineStyle style = HorizontalLine.getDefaultStyle().derive();
        style.setHeight(3);
        style.setColor(Color.fromString("#123456"));
        final HorizontalLine line = new HorizontalLine(style);
        assertEquals("horizontal line", line.getType());
        assertEquals(3, line.getHeight().getValue(), 0.0);
        assertEquals(1, new HorizontalLine().getHeight().getValue(), 0.0);
        assertEquals(Color.fromString("#123456"), line.getColor());
    }

    /**
     * Verifies changes to a bound color and thickness reach the browser.
     */
    @Test
    public void synchronizesLineAppearance() {
        final HorizontalLine line = new HorizontalLine();
        final ColorModel color = new ColorModel(Color.fromString("#cccccc"));
        line.setColorModel(State.NORMAL, color);
        final WidgetSandbox<HorizontalLine> sandbox = WidgetSandbox.open(line);
        sandbox.clearUpdates();
        color.setData(Color.fromString("#777777"));
        line.setHeight(2);
        final var updates = sandbox.drainUpdates();
        assertTrue(updates.stream().anyMatch(update ->
            update.get("action").getStringValue().equals("set color")));
        assertTrue(updates.stream().anyMatch(update ->
            update.get("action").getStringValue().equals("set height")));
    }
}
