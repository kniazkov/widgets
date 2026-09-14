/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.Model;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests fixed reactive carousel sources, selection, and events.
 */
public final class CarouselTest {
    /**
     * Verifies ordered source serialization and the initial first-image selection.
     */
    @Test
    public void serializesSourcesAndSelectsFirstImage() {
        final Carousel carousel = new Carousel("first.png", "second.png");
        assertEquals(2, carousel.getImageCount());
        assertEquals(0, carousel.getSelectedIndex());

        final WidgetSandbox<Carousel> sandbox = WidgetSandbox.open(carousel);
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set carousel sources", carousel
        );
        assertEquals(1, updates.size());
        final JsonArray sources = updates.get(0).get("sources").toJsonArray();
        assertEquals("first.png", sources.getElement(0).getStringValue());
        assertEquals("second.png", sources.getElement(1).getStringValue());
    }

    /**
     * Verifies that a carousel cannot be created without an image.
     */
    @Test
    public void rejectsEmptySourceSequence() {
        assertThrows(IllegalArgumentException.class, () -> new Carousel(List.of()));
    }

    /**
     * Verifies reactive source changes and model replacement at a stable position.
     */
    @Test
    public void sourceModelsReactAndCanBeReplaced() {
        final ImageSourceModel original = new ImageSourceModel(
            ImageSource.fromHyperlink("old.png")
        );
        final Carousel carousel = new Carousel(List.of(original));
        final WidgetSandbox<Carousel> sandbox = WidgetSandbox.open(carousel);
        sandbox.clearUpdates();

        original.setData(ImageSource.fromHyperlink("changed.png"));
        assertSourceUpdate(sandbox, carousel, 0, "changed.png");

        final ImageSourceModel replacement = new ImageSourceModel(
            ImageSource.fromHyperlink("replacement.png")
        );
        carousel.setSourceModel(0, replacement);
        assertSame(replacement, carousel.getSourceModel(0));
        assertSourceUpdate(sandbox, carousel, 0, "replacement.png");

        original.setData(ImageSource.fromHyperlink("detached.png"));
        assertEquals(0, sandbox.drainUpdates().size());
        replacement.setData(ImageSource.fromHyperlink("current.png"));
        assertSourceUpdate(sandbox, carousel, 0, "current.png");

        final List<Model<ImageSource>> models = carousel.getSourceModels();
        assertThrows(UnsupportedOperationException.class,
            () -> models.add(new ImageSourceModel()));
    }

    /**
     * Verifies selection bounds and browser selection event ordering.
     */
    @Test
    public void validatesAndReportsSelection() {
        final Carousel carousel = new Carousel("first.png", "second.png");
        carousel.setSelectedIndex(1);
        assertEquals(1, carousel.getSelectedIndex());
        assertThrows(IllegalArgumentException.class, () -> carousel.setSelectedIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> carousel.setSelectedIndex(2));

        final AtomicInteger received = new AtomicInteger(-1);
        carousel.onSelect(index -> {
            assertEquals(index.intValue(), carousel.getSelectedIndex());
            received.set(index);
        });
        final WidgetSandbox<Carousel> sandbox = WidgetSandbox.open(carousel);
        final JsonObject data = new JsonObject();
        data.addNumber("index", 0);
        sandbox.fire(Event.SELECT, data);
        assertEquals(0, received.get());
        assertEquals(0, carousel.getSelectedIndex());
    }

    /**
     * Extracts and verifies one reactive source update.
     *
     * @param sandbox widget sandbox
     * @param carousel target carousel
     * @param index expected source position
     * @param source expected serialized source
     */
    private static void assertSourceUpdate(
        final WidgetSandbox<Carousel> sandbox,
        final Carousel carousel,
        final int index,
        final String source
    ) {
        final List<JsonObject> updates = WidgetSandbox.findUpdates(
            sandbox.drainUpdates(), "set carousel source", carousel
        );
        assertEquals(1, updates.size());
        assertEquals(index, updates.get(0).get("index").getIntValue());
        assertEquals(source, updates.get(0).get("source").getStringValue());
    }
}
