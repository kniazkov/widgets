/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Immutable original image dimensions in pixels, independent of CSS size limits.
 */
public class IntrinsicSize {
    /**
     * No dimensions supplied; the browser discovers them after loading the image.
     */
    public static final IntrinsicSize NONE = new IntrinsicSize() {
        @Override
        public String toString() {
            return "";
        }
    };

    /**
     * Original width, or zero for NONE.
     */
    private final int width;

    /**
     * Original height, or zero for NONE.
     */
    private final int height;

    /**
     * Constructs only the no-value implementation.
     */
    private IntrinsicSize() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Creates original image dimensions.
     * @param width positive original pixel width
     * @param height positive original pixel height
     */
    public IntrinsicSize(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    /**
     * @return original pixel width, or zero for NONE
     */
    public final int getWidth() {
        return this.width;
    }

    /**
     * @return original pixel height, or zero for NONE
     */
    public final int getHeight() {
        return this.height;
    }

    /**
     * @return whether original dimensions are supplied
     */
    public final boolean isDefined() {
        return this.width > 0;
    }

    @Override
    public String toString() {
        return this.width + " " + this.height;
    }

    @Override
    public final boolean equals(final Object other) {
        return other instanceof IntrinsicSize size
            && this.width == size.width && this.height == size.height;
    }

    @Override
    public final int hashCode() {
        return 31 * this.width + this.height;
    }
}
