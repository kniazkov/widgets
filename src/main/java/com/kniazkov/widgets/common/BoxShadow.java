/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Objects;

/**
 * Immutable CSS box shadow definition measured in pixels.
 */
public final class BoxShadow {
    /**
     * No shadow.
     */
    public static final BoxShadow NONE = new BoxShadow();

    /**
     * Horizontal offset in pixels.
     */
    private final int offsetX;

    /**
     * Vertical offset in pixels.
     */
    private final int offsetY;

    /**
     * Blur radius in pixels.
     */
    private final int blurRadius;

    /**
     * Spread radius in pixels.
     */
    private final int spreadRadius;

    /**
     * Shadow color.
     */
    private final Color color;

    /**
     * Whether the shadow is drawn inside the element.
     */
    private final boolean inset;

    /**
     * Whether this value disables the shadow.
     */
    private final boolean none;

    /**
     * Creates the special value that disables the shadow.
     */
    private BoxShadow() {
        this.offsetX = 0;
        this.offsetY = 0;
        this.blurRadius = 0;
        this.spreadRadius = 0;
        this.color = Color.TRANSPARENT;
        this.inset = false;
        this.none = true;
    }

    /**
     * Creates an outer shadow without a spread radius.
     *
     * @param offsetX horizontal offset in pixels
     * @param offsetY vertical offset in pixels
     * @param blurRadius blur radius in pixels
     * @param color shadow color
     */
    public BoxShadow(final int offsetX, final int offsetY, final int blurRadius,
            final Color color) {
        this(offsetX, offsetY, blurRadius, 0, color, false);
    }

    /**
     * Creates an outer shadow.
     *
     * @param offsetX horizontal offset in pixels
     * @param offsetY vertical offset in pixels
     * @param blurRadius blur radius in pixels
     * @param spreadRadius spread radius in pixels
     * @param color shadow color
     */
    public BoxShadow(final int offsetX, final int offsetY, final int blurRadius,
            final int spreadRadius, final Color color) {
        this(offsetX, offsetY, blurRadius, spreadRadius, color, false);
    }

    /**
     * Creates a shadow.
     *
     * @param offsetX horizontal offset in pixels
     * @param offsetY vertical offset in pixels
     * @param blurRadius blur radius in pixels, not negative
     * @param spreadRadius spread radius in pixels
     * @param color shadow color
     * @param inset whether the shadow is drawn inside the element
     */
    public BoxShadow(final int offsetX, final int offsetY, final int blurRadius,
            final int spreadRadius, final Color color, final boolean inset) {
        if (blurRadius < 0) {
            throw new IllegalArgumentException("Blur radius must be >= 0");
        }
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.blurRadius = blurRadius;
        this.spreadRadius = spreadRadius;
        this.color = Objects.requireNonNull(color, "color");
        this.inset = inset;
        this.none = false;
    }

    /**
     * Returns the horizontal offset.
     *
     * @return horizontal offset in pixels
     */
    public int getOffsetX() {
        return this.offsetX;
    }

    /**
     * Returns the vertical offset.
     *
     * @return vertical offset in pixels
     */
    public int getOffsetY() {
        return this.offsetY;
    }

    /**
     * Returns the blur radius.
     *
     * @return blur radius in pixels
     */
    public int getBlurRadius() {
        return this.blurRadius;
    }

    /**
     * Returns the spread radius.
     *
     * @return spread radius in pixels
     */
    public int getSpreadRadius() {
        return this.spreadRadius;
    }

    /**
     * Returns the shadow color.
     *
     * @return shadow color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Returns whether this is an inset shadow.
     *
     * @return {@code true} for an inset shadow
     */
    public boolean isInset() {
        return this.inset;
    }

    /**
     * Returns CSS code for this shadow.
     *
     * @return CSS box-shadow value
     */
    public String getCSSCode() {
        if (this.none) {
            return "none";
        }
        return (this.inset ? "inset " : "") + this.offsetX + "px " + this.offsetY + "px "
            + this.blurRadius + "px " + this.spreadRadius + "px " + this.color;
    }

    @Override
    public String toString() {
        return this.getCSSCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof BoxShadow)) {
            return false;
        }
        final BoxShadow other = (BoxShadow) obj;
        return this.none == other.none && this.offsetX == other.offsetX
            && this.offsetY == other.offsetY && this.blurRadius == other.blurRadius
            && this.spreadRadius == other.spreadRadius && this.inset == other.inset
            && this.color.equals(other.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.offsetX, this.offsetY, this.blurRadius, this.spreadRadius,
            this.color, this.inset, this.none);
    }
}
