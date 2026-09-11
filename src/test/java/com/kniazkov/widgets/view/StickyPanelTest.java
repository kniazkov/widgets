/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.StickySide;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the sticky block container and its reactive edge setting.
 */
public final class StickyPanelTest {
    /**
     * Verifies the default style and declarative child construction.
     */
    @Test
    public void createsTopStickyBlockContainer() {
        final Section first = new Section(new TextWidget("first"));
        final Section second = new Section(new TextWidget("second"));
        final StickyPanel panel = new StickyPanel(first, second);

        assertTrue(panel instanceof BlockContainer);
        assertEquals("sticky panel", panel.getType());
        assertEquals(StickySide.TOP, panel.getStickySide());
        assertEquals(2, panel.getChildCount());
        assertSame(first, panel.getChild(0));
        assertSame(second, panel.getChild(1));
        assertSame(panel, first.getParent().get());
    }

    /**
     * Verifies that changing the sticky edge is synchronized with the browser.
     */
    @Test
    public void synchronizesBottomSide() {
        final StickyPanel panel = new StickyPanel();
        final WidgetSandbox<StickyPanel> sandbox = WidgetSandbox.open(panel);
        sandbox.clearUpdates();

        panel.stickToBottom();

        final List<JsonObject> updates = sandbox.drainUpdates();
        assertEquals(1, updates.size());
        assertEquals("set sticky side", updates.get(0).get("action").getStringValue());
        assertEquals("bottom", updates.get(0).get("sticky side").getStringValue());
    }

    /**
     * Verifies that derived styles inherit and may override the sticky edge.
     */
    @Test
    public void derivesStickyStyle() {
        final StickyPanelStyle parent = StickyPanel.getDefaultStyle().derive();
        parent.stickToBottom();
        final StickyPanelStyle child = parent.derive();

        assertEquals(StickySide.BOTTOM, child.getStickySide());
        child.stickToTop();
        assertEquals(StickySide.TOP, child.getStickySide());
        assertEquals(StickySide.BOTTOM, parent.getStickySide());
    }
}
