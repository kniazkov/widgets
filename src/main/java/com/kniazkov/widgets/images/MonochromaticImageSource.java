/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * An {@link ImageSource} implementation that generates a monochromatic (single-color) image
 * as an SVG rectangle.
 * <p>
 * This class creates a scalable vector graphic of a solid color rectangle with specified
 * dimensions, suitable for use as placeholders, backgrounds, or simple colored images.
 * The image is encoded as a data URL for direct embedding in HTML or CSS.
 */
public class MonochromaticImageSource implements ImageSource {
    /**
     * The fill color of the generated image.
     */
    private final Color color;

    /**
     * The width of the generated image in pixels.
     */
    private final int width;

    /**
     * The height of the generated image in pixels.
     */
    private final int height;

    /**
     * Constructs a new monochromatic image source.
     *
     * @param color the fill color of the image
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     */
    public MonochromaticImageSource(final Color color, final int width, final int height) {
        this.color = color;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        final String svg = String.format(
            "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d'>" +
            "<rect width='100%%' height='100%%' fill='%s'/>" +
            "</svg>",
            this.width, this.height, this.color.toString()
        );
        String encodedSvg = "";
        try {
            encodedSvg = URLEncoder.encode(
                svg,
                StandardCharsets.UTF_8.name()
            ).replaceAll("\\+", "%20");
        } catch (final UnsupportedEncodingException ignored) {
        }
        return "data:image/svg+xml," + encodedSvg;
    }
}
