/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests direct client-side new-tab hyperlink properties.
 */
public final class NewTabHrefTest {
    /**
     * Verifies the reactive hyperlink exposed by an active image.
     */
    @Test
    public void activeImagePublishesNewTabHref() {
        final ActiveImage image = new ActiveImage("preview.png");
        image.getNewTabHrefModel();
        final WidgetSandbox<ActiveImage> sandbox = WidgetSandbox.open(image);
        sandbox.clearUpdates();

        image.setNewTabHref("original.png");

        assertEquals("original.png", image.getNewTabHref());
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set new tab href", image
        );
        assertEquals(1, updates.size());
        assertEquals(
            "original.png",
            updates.get(0).get("new tab href").getStringValue()
        );
    }
}
