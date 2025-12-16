/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import org.imgscalr.Scalr;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A utility class providing static methods for common image processing operations
 * such as cropping and resizing. All methods work with and return {@link BufferedImage}
 * objects in the ARGB pixel format for consistency.
 */

public final class ImageProcessor {
    /**
     * Private constructor.
     */
    private ImageProcessor() {
    }

    /**
     * Crops the image to a square by taking the largest possible centered square region.
     *
     * @param src the source image to be cropped
     * @return a new square {@link BufferedImage} with the ARGB format
     */
    public static BufferedImage cropToSquare(final BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);

        int x = (w - side) / 2;
        int y = (h - side) / 2;

        final BufferedImage sub = src.getSubimage(x, y, side, side);
        return copyAsARGB(sub);
    }

    /**
     * Resizes an image to fit within a square bounding box of the specified size,
     * preserving aspect ratio. Does not upscale images that are already smaller.
     *
     * @param src the source image to be resized
     * @param maxSize the maximum width and height of the resulting image
     * @return a new {@link BufferedImage} with the ARGB format, resized if necessary
     * @throws IllegalArgumentException if {@code maxSize} is not positive
     */
    public static BufferedImage resizeToFit(final BufferedImage src, final int maxSize) {
        return resizeToFit(src, maxSize, false);
    }

    /**
     * Resizes an image to fit within a square bounding box of the specified size,
     * preserving aspect ratio. Allows control over upscaling behavior.
     *
     * @param src  the source image to be resized
     * @param maxSize the maximum width and height of the resulting image
     * @param allowUpscale if {@code true}, smaller images will be enlarged to fit the box;
     *  if {@code false}, they will be returned unchanged
     * @return a new {@link BufferedImage} with the ARGB format, resized according to the rules
     * @throws IllegalArgumentException if {@code maxSize} is not positive
     */

    public static BufferedImage resizeToFit(final BufferedImage src, final int maxSize,
            final boolean allowUpscale) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        int w = src.getWidth();
        int h = src.getHeight();
        if (!allowUpscale && w <= maxSize && h <= maxSize) {
            return copyAsARGB(src);
        }
        final Scalr.Mode mode = (w >= h) ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
        final  BufferedImage argb = (src.getType() == BufferedImage.TYPE_INT_ARGB) ?
            src : copyAsARGB(src);
        final BufferedImage scaled = Scalr.resize(
            argb,
            Scalr.Method.ULTRA_QUALITY,
            mode,
            maxSize,
            maxSize,
            Scalr.OP_ANTIALIAS
        );
        return (scaled.getType() == BufferedImage.TYPE_INT_ARGB) ? scaled : copyAsARGB(scaled);
    }

    /**
     * Creates a copy of the given image, ensuring it uses the {@link BufferedImage#TYPE_INT_ARGB}
     * format. All rendering hints are set for maximum quality during the conversion.
     *
     * @param src the source image to copy
     * @return an ARGB-format copy of the source image
     */
    private static BufferedImage copyAsARGB(final BufferedImage src) {
        final BufferedImage dst = new BufferedImage(
            src.getWidth(),
            src.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D g = dst.createGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }
}
