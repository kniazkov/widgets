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
     * @param src the source image with ARGB format (will be copied, not modified)
     * @param sourceColor the color to replace (including alpha component)
     * @param targetColor the color to replace with (including alpha component)
     * @return a new {@link BufferedImage} with the specified color replaced
     * @throws IllegalArgumentException if any parameter is null
     */
    public static BufferedImage replaceColor(final BufferedImage src,
                                             final Color sourceColor,
                                             final Color targetColor) {
        if (src == null) {
            throw new IllegalArgumentException("Source image cannot be null");
        }
        if (sourceColor == null) {
            throw new IllegalArgumentException("Source color cannot be null");
        }
        if (targetColor == null) {
            throw new IllegalArgumentException("Target color cannot be null");
        }

        // Ensure we work with ARGB format
        final BufferedImage result = copyAsARGB(src);

        // Convert colors to packed integers for faster comparison
        final int sourceRGB = packRGBA(sourceColor.getRed(), sourceColor.getGreen(),
                                      sourceColor.getBlue(), sourceColor.getAlpha());
        final int targetRGB = packRGBA(targetColor.getRed(), targetColor.getGreen(),
                                      targetColor.getBlue(), targetColor.getAlpha());

        // Get image dimensions
        final int width = result.getWidth();
        final int height = result.getHeight();

        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = result.getRGB(x, y);

                // Replace exact match (including alpha)
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
     *
     * @param src the source image with ARGB format (will be copied, not modified)
     * @param sourceColor the color to replace (alpha component is ignored during matching)
     * @param targetColor the color to replace with (alpha component will be used)
     * @return a new {@link BufferedImage} with the specified color replaced
     * @throws IllegalArgumentException if any parameter is null
     */
    public static BufferedImage replaceColorIgnoreAlpha(final BufferedImage src,
                                                        final Color sourceColor,
                                                        final Color targetColor) {
        if (src == null) {
            throw new IllegalArgumentException("Source image cannot be null");
        }
        if (sourceColor == null) {
            throw new IllegalArgumentException("Source color cannot be null");
        }
        if (targetColor == null) {
            throw new IllegalArgumentException("Target color cannot be null");
        }

        // Ensure we work with ARGB format
        final BufferedImage result = copyAsARGB(src);

        // Mask to ignore alpha during comparison (0x00FFFFFF)
        final int RGB_MASK = 0x00FFFFFF;

        // Convert colors to packed integers
        final int sourceRGB = packRGBA(sourceColor.getRed(), sourceColor.getGreen(),
                                      sourceColor.getBlue(), sourceColor.getAlpha()) & RGB_MASK;
        final int targetRGB = packRGBA(targetColor.getRed(), targetColor.getGreen(),
                                      targetColor.getBlue(), targetColor.getAlpha());

        // Get image dimensions
        final int width = result.getWidth();
        final int height = result.getHeight();

        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = result.getRGB(x, y);

                // Compare only RGB components (ignore alpha)
                if ((pixel & RGB_MASK) == sourceRGB) {
                    // Preserve original alpha from targetColor, not from source pixel
                    result.setRGB(x, y, targetRGB);
                }
            }
        }

        return result;
    }

    /**
     * Replaces colors within a specified tolerance range around the source color.
     * This is useful for replacing similar colors or anti-aliased edges.
     *
     * @param src the source image with ARGB format (will be copied, not modified)
     * @param sourceColor the reference color to replace
     * @param targetColor the color to replace with
     * @param tolerance the maximum allowed difference for each color component (0-255)
     * @param includeAlpha whether to consider alpha channel in tolerance calculation
     * @return a new {@link BufferedImage} with colors within tolerance replaced
     * @throws IllegalArgumentException if any parameter is null or tolerance is invalid
     */
    public static BufferedImage replaceColorWithTolerance(final BufferedImage src,
                                                         final Color sourceColor,
                                                         final Color targetColor,
                                                         final int tolerance,
                                                         final boolean includeAlpha) {
        if (src == null) {
            throw new IllegalArgumentException("Source image cannot be null");
        }
        if (sourceColor == null) {
            throw new IllegalArgumentException("Source color cannot be null");
        }
        if (targetColor == null) {
            throw new IllegalArgumentException("Target color cannot be null");
        }
        if (tolerance < 0 || tolerance > 255) {
            throw new IllegalArgumentException("Tolerance must be between 0 and 255");
        }

        // Ensure we work with ARGB format
        final BufferedImage result = copyAsARGB(src);

        // Convert colors to packed integers
        final int sourceRGB = packRGBA(sourceColor.getRed(), sourceColor.getGreen(),
                                      sourceColor.getBlue(), sourceColor.getAlpha());
        final int targetRGB = packRGBA(targetColor.getRed(), targetColor.getGreen(),
                                      targetColor.getBlue(), targetColor.getAlpha());

        // Get individual components for comparison
        final int sourceR = sourceColor.getRed();
        final int sourceG = sourceColor.getGreen();
        final int sourceB = sourceColor.getBlue();
        final int sourceA = sourceColor.getAlpha();

        // Get image dimensions
        final int width = result.getWidth();
        final int height = result.getHeight();

        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = result.getRGB(x, y);

                // Check if pixel is within tolerance
                if (isColorWithinTolerance(pixel, sourceR, sourceG, sourceB, sourceA,
                                          tolerance, includeAlpha)) {
                    result.setRGB(x, y, targetRGB);
                }
            }
        }

        return result;
    }

    /**
     * Helper method to pack RGBA components into a single integer.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @param a alpha component (0-255)
     * @return packed integer in ARGB format
     */
    private static int packRGBA(final int r, final int g, final int b, final int a) {
        return ((a & 0xFF) << 24) |
               ((r & 0xFF) << 16) |
               ((g & 0xFF) << 8) |
               (b & 0xFF);
    }

    /**
     * Helper method to check if a pixel color is within tolerance of a reference color.
     *
     * @param pixel packed pixel value in ARGB format
     * @param refR reference red component
     * @param refG reference green component
     * @param refB reference blue component
     * @param refA reference alpha component
     * @param tolerance maximum allowed difference for each component
     * @param includeAlpha whether to consider alpha channel
     * @return true if pixel is within tolerance, false otherwise
     */
    private static boolean isColorWithinTolerance(final int pixel,
                                                 final int refR, final int refG,
                                                 final int refB, final int refA,
                                                 final int tolerance,
                                                 final boolean includeAlpha) {
        // Extract components from pixel
        final int pixelA = (pixel >> 24) & 0xFF;
        final int pixelR = (pixel >> 16) & 0xFF;
        final int pixelG = (pixel >> 8) & 0xFF;
        final int pixelB = pixel & 0xFF;

        // Check RGB components
        if (Math.abs(pixelR - refR) > tolerance ||
            Math.abs(pixelG - refG) > tolerance ||
            Math.abs(pixelB - refB) > tolerance) {
            return false;
        }

        // Check alpha if requested
        if (includeAlpha && Math.abs(pixelA - refA) > tolerance) {
            return false;
        }

        return true;
    }
}
