/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.protocol.Update;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Per-page metadata inherits options and participates in reactive property updates.
 */
public class RootMetadataTest {
    /**
     * Page overrides are isolated; empty means remove and reset restores site defaults.
     */
    @Test
    public void inheritsOverridesAndResets() {
        final Options options = new Options.Builder().setTitle("Shop")
            .setDescription("Site description").setRobots("index, follow").build();
        final RootWidget first = new RootWidget(options);
        final RootWidget second = new RootWidget(options);
        assertEquals("Shop", first.getTitle());
        assertEquals("Site description", first.getDescription());
        assertEquals("index, follow", first.getRobots());
        first.setTitle("Product");
        first.setDescription("");
        first.setRobots("noindex");
        assertEquals("Product", first.getTitle());
        assertEquals("", first.getDescription());
        assertEquals("Shop", second.getTitle());
        assertEquals("Shop", options.getTitle());
        first.resetMetadata();
        assertEquals("Shop", first.getTitle());
        assertEquals("Site description", first.getDescription());
        assertEquals("index, follow", first.getRobots());
    }

    /**
     * External models send updates, survive GC and detach when reset or rebound.
     */
    @Test
    public void reactsAndDetaches() {
        final RootWidget root = new RootWidget();
        final StringModel title = new StringModel("First");
        final StringModel description = new StringModel("Description");
        final StringModel robots = new StringModel("noindex");
        root.setTitleModel(title);
        root.setDescriptionModel(description);
        root.setRobotsModel(robots);
        final TreeSet<Update> updates = new TreeSet<>();
        root.getUpdates(updates);
        updates.clear();
        System.gc();
        title.setData("Second");
        description.setData("Changed");
        robots.setData("index");
        root.getUpdates(updates);
        assertEquals(3, updates.size());
        assertTrue(updates.toString().contains("set document title"));
        assertTrue(updates.toString().contains("Second"));
        root.resetMetadata();
        root.getUpdates(updates);
        updates.clear();
        title.setData("Detached");
        description.setData("Detached");
        robots.setData("Detached");
        root.getUpdates(updates);
        assertTrue(updates.isEmpty());
        assertEquals("", root.getTitle());
        assertThrows(NullPointerException.class, () -> root.setTitle(null));
        assertThrows(NullPointerException.class, () -> root.setDescriptionModel(null));
    }
}
