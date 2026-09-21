/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.IntrinsicSize;
import com.kniazkov.widgets.model.IntrinsicSizeModel;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Intrinsic metadata is independent of CSS size limits and uses the normal property protocol.
 */
public class IntrinsicSizeTest {
    /**
     * Dimensions validate atomically and clearing restores the absence of metadata.
     */
    @Test
    public void reservesAndClearsDimensions() {
        final ImageWidget image = new ImageWidget("/logo.svg");
        image.setMaxWidth(200);
        image.setMaxHeight(80);
        image.setIntrinsicSize(350, 100);
        assertEquals(new IntrinsicSize(350, 100), image.getIntrinsicSize());
        assertEquals("\"350 100\"",
            Property.INTRINSIC_SIZE.convertData(image.getIntrinsicSize()).toString());
        assertThrows(IllegalArgumentException.class, () -> image.setIntrinsicSize(0, 10));
        assertThrows(IllegalArgumentException.class, () -> image.setIntrinsicSize(10, -1));
        assertEquals(new IntrinsicSize(350, 100), image.getIntrinsicSize());
        image.clearIntrinsicSize();
        assertSame(IntrinsicSize.NONE, image.getIntrinsicSize());
        assertEquals("\"\"",
            Property.INTRINSIC_SIZE.convertData(image.getIntrinsicSize()).toString());
        assertEquals("200.0px", image.getMaxWidth().toString());
    }

    /**
     * The typed property binds a replaceable model and suppresses equal-value updates.
     */
    @Test
    public void bindsTypedModelWithNoValueDefault() {
        assertEquals(IntrinsicSize.class, Property.INTRINSIC_SIZE.getValueClass());
        assertTrue(Property.INTRINSIC_SIZE.createDefaultModel() instanceof IntrinsicSizeModel);
        final ImageWidget image = new ImageWidget("/logo.svg");
        assertSame(IntrinsicSize.NONE, image.getIntrinsicSize());
        final IntrinsicSizeModel model = new IntrinsicSizeModel();
        final AtomicInteger changes = new AtomicInteger();
        model.addListener(size -> changes.incrementAndGet());
        image.setIntrinsicSizeModel(model);
        assertSame(model, image.getIntrinsicSizeModel());
        final IntrinsicSize size = new IntrinsicSize(350, 100);
        image.setIntrinsicSize(size);
        assertSame(size, image.getIntrinsicSize());
        assertFalse(model.setData(new IntrinsicSize(350, 100)));
        assertEquals(1, changes.get());
        image.clearIntrinsicSize();
        assertEquals(2, changes.get());
        assertSame(IntrinsicSize.NONE, model.getData());
        assertEquals(size, model.deriveWithData(size).getData());
        assertSame(IntrinsicSize.NONE, model.getDefaultData());
        assertThrows(NullPointerException.class, () -> model.setData(null));
        assertThrows(NullPointerException.class, () -> new IntrinsicSizeModel(null));
    }

    /**
     * No-value metadata is distinct from valid positive dimensions.
     */
    @Test
    public void representsOriginalDimensionsAndTheirAbsence() {
        final IntrinsicSize size = new IntrinsicSize(350, 100);
        assertEquals(350, size.getWidth());
        assertEquals(100, size.getHeight());
        assertTrue(size.isDefined());
        assertEquals(size.hashCode(), new IntrinsicSize(350, 100).hashCode());
        assertFalse(IntrinsicSize.NONE.isDefined());
        assertEquals(0, IntrinsicSize.NONE.getWidth());
        assertEquals(0, IntrinsicSize.NONE.getHeight());
        assertEquals("", IntrinsicSize.NONE.toString());
        assertFalse(size.equals(IntrinsicSize.NONE));
        assertFalse(IntrinsicSize.NONE.equals(size));
        assertThrows(IllegalArgumentException.class, () -> new IntrinsicSize(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new IntrinsicSize(-1, 10));
    }
}
