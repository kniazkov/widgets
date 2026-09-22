/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.RealNumberModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Reactive gesture properties participate in ordinary style inheritance and model binding.
 */
public final class GestureStyleTest {
    /**
     * Default styles explicitly expose both settings through the shared property registry.
     */
    @Test
    public void defaultsArePropertiesAndAreSentAtConstruction() {
        assertSame(SortableSectionStyle.DEFAULT, SortableSection.getDefaultStyle());
        assertSame(ZoomDecoratorStyle.DEFAULT, ZoomDecorator.getDefaultStyle());
        assertEquals(Integer.valueOf(250), SortableSectionStyle.DEFAULT
            .getModel(State.ANY, Property.ANIMATION_DURATION).getData());
        assertEquals(Double.valueOf(8), ZoomDecoratorStyle.DEFAULT
            .getModel(State.ANY, Property.MAX_SCALE).getData());
        assertTrue(Property.ANIMATION_DURATION.createDefaultModel().isValid());
        assertTrue(Property.MAX_SCALE.createDefaultModel().isValid());
        final SortableSection sorting = new SortableSection();
        final ZoomDecorator zoom = new ZoomDecorator();
        assertEquals(250, WidgetSandbox.findUpdates(WidgetSandbox.open(sorting).drainUpdates(),
            "set animation duration", sorting).get(0).get("animation duration").getIntValue());
        assertEquals(8, WidgetSandbox.findUpdates(WidgetSandbox.open(zoom).drainUpdates(),
            "set max scale", zoom).get(0).get("max scale").getIntValue());
    }

    /**
     * Cascaded duration changes propagate until a widget overrides its own model.
     */
    @Test
    public void sortingInheritsOverridesAndReplacesStyles() {
        final SortableSectionStyle parent = SortableSectionStyle.DEFAULT.derive();
        final SortableSectionStyle derived = parent.derive();
        final SortableSection first = new SortableSection(derived);
        final SortableSection second = new SortableSection(derived);
        parent.getAnimationDurationModel().setData(600);
        assertEquals(600, first.getAnimationDuration());
        assertEquals(600, second.getAnimationDuration());
        first.setAnimationDuration(100);
        parent.setAnimationDuration(400);
        assertEquals(100, first.getAnimationDuration());
        assertEquals(400, second.getAnimationDuration());
        assertEquals(250, SortableSectionStyle.DEFAULT.getAnimationDuration());
        final SortableSectionStyle replacement = SortableSectionStyle.DEFAULT.derive();
        replacement.setAnimationDuration(0);
        first.setStyle(replacement);
        assertEquals(0, first.getAnimationDuration());
        replacement.setAnimationDuration(150);
        assertEquals(150, first.getAnimationDuration());
    }

    /**
     * Zoom uses the same cascading behavior and a distinct typed style.
     */
    @Test
    public void zoomInheritsOverridesAndReplacesStyles() {
        final ZoomDecoratorStyle parent = ZoomDecoratorStyle.DEFAULT.derive();
        final ZoomDecoratorStyle derived = parent.derive();
        final ZoomDecorator first = new ZoomDecorator(derived, new TextWidget());
        final ZoomDecorator second = new ZoomDecorator(derived, new TextWidget());
        parent.getMaxScaleModel().setData(6.0);
        assertEquals(6, first.getMaxScale(), 0);
        assertEquals(6, second.getMaxScale(), 0);
        first.setMaxScale(2);
        parent.setMaxScale(4);
        assertEquals(2, first.getMaxScale(), 0);
        assertEquals(4, second.getMaxScale(), 0);
        assertEquals(8, ZoomDecoratorStyle.DEFAULT.getMaxScale(), 0);
        final ZoomDecoratorStyle replacement = ZoomDecoratorStyle.DEFAULT.derive();
        replacement.setMaxScale(3);
        first.setStyle(replacement);
        replacement.setMaxScale(5);
        assertEquals(5, first.getMaxScale(), 0);
    }

    /**
     * Model replacement detaches the previous source and sends ordinary property updates.
     */
    @Test
    public void widgetsReactToReplacementModels() {
        final SortableSection sorting = new SortableSection();
        final ZoomDecorator zoom = new ZoomDecorator();
        final WidgetSandbox<SortableSection> sortingSandbox = WidgetSandbox.open(sorting);
        final WidgetSandbox<ZoomDecorator> zoomSandbox = WidgetSandbox.open(zoom);
        final IntegerModel duration = new IntegerModel(500);
        final RealNumberModel limit = new RealNumberModel(7.0);
        sorting.setAnimationDurationModel(duration);
        zoom.setMaxScaleModel(limit);
        sortingSandbox.clearUpdates();
        zoomSandbox.clearUpdates();
        duration.setData(300);
        limit.setData(4.0);
        assertEquals(300, sortingSandbox.drainUpdates().get(0)
            .get("animation duration").getIntValue());
        assertEquals(4, zoomSandbox.drainUpdates().get(0).get("max scale").getIntValue());
        sorting.setAnimationDurationModel(new IntegerModel(200));
        zoom.setMaxScaleModel(new RealNumberModel(2.0));
        sortingSandbox.clearUpdates();
        zoomSandbox.clearUpdates();
        duration.setData(900);
        limit.setData(9.0);
        assertTrue(sortingSandbox.drainUpdates().isEmpty());
        assertTrue(zoomSandbox.drainUpdates().isEmpty());
        assertThrows(IllegalArgumentException.class,
            () -> sorting.setAnimationDurationModel(new IntegerModel(-1)));
        assertThrows(IllegalArgumentException.class,
            () -> zoom.setMaxScaleModel(new RealNumberModel(Double.NaN)));
    }

    /**
     * Rebinding a style model preserves the inherited listeners on existing widgets.
     */
    @Test
    public void styleModelReplacementReachesExistingWidgets() {
        final SortableSectionStyle sortingStyle = SortableSectionStyle.DEFAULT.derive();
        final ZoomDecoratorStyle zoomStyle = ZoomDecoratorStyle.DEFAULT.derive();
        final SortableSection sorting = new SortableSection(sortingStyle.derive());
        final ZoomDecorator zoom = new ZoomDecorator(zoomStyle.derive(), new TextWidget());
        final IntegerModel duration = new IntegerModel(450);
        final RealNumberModel limit = new RealNumberModel(6.0);
        sortingStyle.setAnimationDurationModel(duration);
        zoomStyle.setMaxScaleModel(limit);
        duration.setData(550);
        limit.setData(7.0);
        assertEquals(550, sorting.getAnimationDuration());
        assertEquals(7, zoom.getMaxScale(), 0);
    }
}
