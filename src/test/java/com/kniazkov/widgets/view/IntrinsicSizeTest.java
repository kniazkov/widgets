/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
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
        assertEquals("350 100", image.getModel(State.ANY, Property.INTRINSIC_SIZE).getData());
        assertEquals("\"350 100\"", Property.INTRINSIC_SIZE.convertData("350 100").toString());
        assertThrows(IllegalArgumentException.class, () -> image.setIntrinsicSize(0, 10));
        assertThrows(IllegalArgumentException.class, () -> image.setIntrinsicSize(10, -1));
        assertEquals("350 100", image.getModel(State.ANY, Property.INTRINSIC_SIZE).getData());
        image.clearIntrinsicSize();
        assertEquals("", image.getModel(State.ANY, Property.INTRINSIC_SIZE).getData());
        assertEquals("200.0px", image.getMaxWidth().toString());
    }
}
