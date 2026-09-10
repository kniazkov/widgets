/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Tests immutable Google Fonts configuration and its reusable font face.
 */
public final class GoogleFontTest {
    /**
     * The CSS API request is deterministic and the font face remains reusable.
     */
    @Test
    public void createsStylesheetAndFontFace() {
        final GoogleFont font = new GoogleFont(
            "Roboto Slab",
            FontWeight.BOLD,
            FontWeight.NORMAL,
            FontWeight.BOLD
        );

        assertEquals("Roboto Slab", font.getFamily());
        assertArrayEquals(new int[] {400, 700}, font.getWeights());
        assertEquals(
            "https://fonts.googleapis.com/css2?family=Roboto+Slab:wght@400;700&display=swap",
            font.getStylesheetUri().toString()
        );
        assertEquals("'Roboto Slab'", font.getFontFace().getName());
        assertSame(font.getFontFace(), font.getFontFace());
    }

    /**
     * Invalid or underspecified requests fail before the server starts.
     */
    @Test
    public void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new GoogleFont(" "));
        assertThrows(
            IllegalArgumentException.class,
            () -> new GoogleFont("Roboto", new FontWeight[0])
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new GoogleFont("Roboto; color:red")
        );
    }
}
