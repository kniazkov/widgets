/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.List;
import com.kniazkov.widgets.model.BooleanModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Generic decoration, ownership, scale limits and viewport reset protocol.
 */
public final class ZoomDecoratorTest {
    /**
     * Arbitrary inline content can be replaced or removed while maintaining one child.
     */
    @Test
    public void wrapsAndReplacesInlineContent() {
        final TextWidget text = new TextWidget("Original");
        final Section oldParent = new Section(text);
        final ZoomDecorator zoom = new ZoomDecorator(text);
        assertEquals(0, oldParent.getChildCount());
        assertSame(text, zoom.getChild());
        assertSame(zoom, text.getParent().orElseThrow());
        final WidgetSandbox<ZoomDecorator> sandbox = WidgetSandbox.open(zoom);
        sandbox.clearUpdates();
        zoom.put(text);
        assertTrue(sandbox.drainUpdates().isEmpty());
        final InlineBlock composite = new InlineBlock(new Section(new TextWidget("Nested")));
        zoom.put(composite);
        assertTrue(text.getParent().isEmpty());
        assertSame(composite, zoom.getChild());
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set child", composite
        ).size());
        zoom.removeAll();
        assertEquals(1, zoom.getChildCount());
        assertTrue(zoom.getChild() instanceof TextWidget);
        assertTrue(composite.getParent().isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> zoom.getChild(1));
    }

    /**
     * Limits reject invalid values and publish resets without changing child identity.
     */
    @Test
    public void validatesLimitsAndResetsViewport() {
        final ZoomDecorator zoom = new ZoomDecorator();
        final WidgetSandbox<ZoomDecorator> sandbox = WidgetSandbox.open(zoom);
        sandbox.clearUpdates();
        assertEquals(8, zoom.getMaxScale(), 0);
        for (final double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> zoom.setMaxScale(invalid));
        }
        assertTrue(sandbox.drainUpdates().isEmpty());
        zoom.setMaxScale(3);
        zoom.resetZoom();
        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(2, updates.size());
        assertEquals("set max scale", updates.get(0).get("action").getStringValue());
        assertEquals(3, updates.get(0).get("max scale").getIntValue());
        assertEquals("reset zoom", updates.get(1).get("action").getStringValue());
        assertEquals(3, zoom.getMaxScale(), 0);
        zoom.setMaxScale(1);
        assertEquals(1, zoom.getMaxScale(), 0);
    }

    /**
     * Invalid replacements cannot corrupt the existing hierarchy.
     */
    @Test
    public void rejectsNullAndCycles() {
        final ZoomDecorator zoom = new ZoomDecorator();
        final InlineWidget<?> original = zoom.getChild();
        assertThrows(NullPointerException.class, () -> zoom.put(null));
        assertThrows(IllegalArgumentException.class, () -> zoom.put(zoom));
        final InlineBlock ancestor = new InlineBlock(new Section(zoom));
        assertThrows(IllegalArgumentException.class, () -> zoom.put(ancestor));
        assertSame(original, zoom.getChild());
    }
    /**
     * Fitting is opt-in, inherits from styles and follows replacement models.
     */
    @Test
    public void fittingUsesReactiveProperty() {
        assertFalse(ZoomDecoratorStyle.DEFAULT.isFitContent());
        final ZoomDecoratorStyle style = ZoomDecoratorStyle.DEFAULT.derive();
        final ZoomDecorator zoom = new ZoomDecorator(style, new TextWidget("Content"));
        final WidgetSandbox<ZoomDecorator> sandbox = WidgetSandbox.open(zoom);
        sandbox.clearUpdates();
        style.setFitContent(true);
        assertTrue(zoom.isFitContent());
        assertEquals(1, WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set fit content", zoom
        ).size());
        final BooleanModel model = new BooleanModel(false);
        zoom.setFitContentModel(model);
        assertFalse(zoom.isFitContent());
        model.setData(true);
        assertTrue(zoom.isFitContent());
    }

}
