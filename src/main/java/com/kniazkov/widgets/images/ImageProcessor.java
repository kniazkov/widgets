/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.Color;
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

    /**
     * Replaces all pixels of a specified source color with a target color in an image.
     * Only pixels that exactly match the source color (including alpha) will be replaced.
     * The operation preserves the original image's dimensions and transparency information.
     *
     * @param image the source image with ARGB format (will be copied, not modified)
     * @param sourceColor the color to replace (including alpha component)
     * @param targetColor the color to replace with (including alpha component)
     * @return a new {@link BufferedImage} with the specified color replaced
     */
    public static BufferedImage replaceColor(final BufferedImage image,
            final Color sourceColor, final Color targetColor) {
        final BufferedImage result = copyAsARGB(image);
        final int sourceRGB = sourceColor.pack();
        final int targetRGB = targetColor.pack();
        final int width = result.getWidth();
        final int height = result.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = result.getRGB(x, y);
                if (pixel == sourceRGB) {
                    result.setRGB(x, y, targetRGB);
                }
            }
        }
        return result;
    }

    /**
     * Replaces all pixels of a specified source color with a target color in an image,
     * ignoring alpha channel during color matching. Only the RGB components are compared,
     * while alpha transparency is preserved from the original pixel.
     * @param image the source image with ARGB format (will be copied, not modified)
     * @param sourceColor the color to replace (alpha component is ignored during matching)
     * @param targetColor the color to replace with (alpha component will be combined with original)
     * @return a new {@link BufferedImage} with the specified color replaced
     */
    public static BufferedImage replaceColorIgnoreAlpha(final BufferedImage image,
            final Color sourceColor, final Color targetColor) {
        final BufferedImage result = copyAsARGB(image);
        final int mask = 0x00FFFFFF;
        final int sourceRGB = sourceColor.pack() & mask;
        final int targetRGB = targetColor.pack() & mask;
        final int targetAlpha = targetColor.getAlpha();
        final int width = result.getWidth();
        final int height = result.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = result.getRGB(x, y);
                if ((pixel & mask) == sourceRGB) {
                    final int origAlpha = (pixel >>> 24) & 0xFF;
                    if (origAlpha == 0) {
                        continue;
                    }
                    final int finalAlpha = (origAlpha * targetAlpha + 127) / 255;
                    if (targetAlpha == 0) {
                        result.setRGB(x, y, 0x00000000);
                    } else {
                        int finalColor = (finalAlpha << 24) | targetRGB;
                        result.setRGB(x, y, finalColor);
                    }
                }
            }
        }
        return result;
    }
}
