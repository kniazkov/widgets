/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.net.URI;

/**
 * Describes a browser font loaded from an external CSS stylesheet.
 *
 * <p>The stylesheet is connected to every application page through
 * {@code Options}. The returned {@link FontFace} can then be assigned to any
 * text widget or style.</p>
 */
public interface WebFont {
    /**
     * Returns the external CSS stylesheet that declares the font.
     *
     * @return stylesheet URI
     */
    URI getStylesheetUri();

    /**
     * Returns the font face used by widget styles after the stylesheet loads.
     *
     * @return reusable font face
     */
    FontFace getFontFace();
}
