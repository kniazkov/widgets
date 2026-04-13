/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * An abstract {@link ImageSource} implementation that represents an SVG image.
 * Subclasses provide raw SVG markup, while this class converts it into a data URL.
 */
public abstract class SvgImageSource implements ImageSource {
    /**
     * An empty SVG image source containing a valid SVG document with no visible content.
     */
    public static final SvgImageSource EMPTY = new SvgImageSource() {
        /**
         * Returns an empty but valid SVG document.
         *
         * @return SVG markup as a string
         */
        @Override
        protected String getSvg() {
            return "<svg xmlns='http://www.w3.org/2000/svg'></svg>";
        }
    };

    /**
     * Returns the raw SVG markup for this image.
     *
     * @return SVG document as a string
     */
    protected abstract String getSvg();

    /**
     * Returns this SVG image as a data URL string.
     *
     * @return encoded SVG data URL
     */
    @Override
    public String toString() {
        try {
            return "data:image/svg+xml," + URLEncoder.encode(
                    getSvg(),
                    StandardCharsets.UTF_8.name()
            ).replaceAll("\\+", "%20");
        } catch (final UnsupportedEncodingException ignored) {
            return "";
        }
    }
}
