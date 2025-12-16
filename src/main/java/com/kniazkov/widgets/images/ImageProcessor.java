/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import org.imgscalr.Scalr;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ImageProcessor {
    /**
     * ...
     * @param src
     * @return
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
     * ...
     * @param src
     * @param maxSize
     * @return
     */
    public static BufferedImage resizeToFit(final BufferedImage src, final int maxSize) {
        return resizeToFit(src, maxSize, false);
    }

    /**
     * ...
     * @param src
     * @param maxSize
     * @param allowUpscale
     * @return
     */
    public static BufferedImage resizeToFit(BufferedImage src, int maxSize, boolean allowUpscale) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");

        int w = src.getWidth();
        int h = src.getHeight();

        if (!allowUpscale && w <= maxSize && h <= maxSize) {
            return copyAsARGB(src);
        }

        Scalr.Mode mode = (w >= h) ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;

        BufferedImage argb = (src.getType() == BufferedImage.TYPE_INT_ARGB) ? src : copyAsARGB(src);

        BufferedImage scaled = Scalr.resize(
            argb,
            Scalr.Method.ULTRA_QUALITY,
            mode,
            maxSize,
            maxSize,
            Scalr.OP_ANTIALIAS
        );

        // на всякий: гарантируем ARGB
        return (scaled.getType() == BufferedImage.TYPE_INT_ARGB) ? scaled : copyAsARGB(scaled);
    }

    private static BufferedImage copyAsARGB(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }
}
