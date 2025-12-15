/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.awt.image.BufferedImage;

/**
 * Represents a source of an image. Any implementation must be convertible to a string
 * so that it can be written into the {@code src} attribute of an {@code <img>} element.
 */
public interface ImageSource {
    @Override
    String toString();

    /**
     * Creates an {@link ImageSource} backed by a plain hyperlink string.
     *
     * @param href the raw hyperlink pointing to the image
     * @return an {@code ImageSource} that returns the given hyperlink from {@code toString()}
     */
    static ImageSource fromHyperlink(final String href) {
        return new ImageSource() {
            @Override
            public String toString() {
                return href;
            }
        };
    }

    /**
     * Creates an {@link ImageSource} backed by a {@link BufferedImage} object.
     * The image data will be converted to an internal representation (e.g., a data URL).
     *
     * @param image the {@code BufferedImage} object to be used as the source
     * @return an {@code ImageSource} that contains the converted image data
     */
    static ImageSource fromImage(final BufferedImage image) {
        return new BufferedImageSource(image);
    }

    /**
     * A sentinel {@link ImageSource} representing an invalid or unavailable image.
     * Its string value resolves to {@code "#"}.
     */
    ImageSource INVALID = new ImageSource() {
        @Override
        public String toString() {
            return "#";
        }
    };
}
