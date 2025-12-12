/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

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
