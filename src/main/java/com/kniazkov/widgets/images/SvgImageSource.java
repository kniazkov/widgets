/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * An abstract {@link ImageSource} implementation that represents an SVG image.
 * <p>
 * Subclasses generate raw SVG markup as a string, while this base class handles
 * conversion of that markup into a {@code data:image/svg+xml,...} URL suitable
 * for use in HTML or CSS.
 */
public abstract class SvgImageSource implements ImageSource {
    /**
     * Returns the raw SVG markup for this image.
     *
     * @return SVG document as a string
     */
    protected abstract String getSvg();

    /**
     * Converts this SVG image to a data URL string.
     *
     * @return a {@code data:image/svg+xml,...} URL containing the encoded SVG markup
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
