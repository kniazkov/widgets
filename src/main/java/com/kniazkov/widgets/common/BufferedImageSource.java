/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * An {@link ImageSource} implementation that provides a Base64-encoded PNG image
 * from an in-memory {@link BufferedImage}.
 * <p>
 * This class converts a Java {@link BufferedImage} to a data URL format suitable
 * for direct embedding in HTML or CSS (e.g., {@code src} attribute of an {@code <img>} tag).
 */
public class BufferedImageSource implements ImageSource {
    /**
     * The source image stored in memory.
     */
    private final BufferedImage image;

    /**
     * Constructs a new image source from the specified buffered image.
     *
     * @param image the image to encode and serve
     */
    public BufferedImageSource(final BufferedImage image) {
        this.image = image;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", stream);
        } catch (final IOException ignored) {
            return "";
        }
        return "data:image/png;base64," +
               Base64.getEncoder().encodeToString(stream.toByteArray());
    }
}
