/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;

/**
 * An {@link ImageSource} implementation that generates a monochromatic (single-color) image
 * as an SVG rectangle.
 */
public class MonochromaticImageSource extends SvgImageSource {
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

    /**
     * Builds the raw SVG markup for the monochromatic image.
     *
     * @return SVG document as a string
     */
    @Override
    protected String getSvg() {
        return String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d'>" +
                        "<rect width='100%%' height='100%%' fill='%s'/>" +
                        "</svg>",
                this.width,
                this.height,
                this.color.toString()
        );
    }
}
