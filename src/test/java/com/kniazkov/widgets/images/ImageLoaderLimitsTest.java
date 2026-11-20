/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the decode guard with ordinary images and deliberately small test quotas.
 */
public class ImageLoaderLimitsTest {
    /**
     * Header, pixel and workspace quotas are independent of the encoded size.
     */
    @Test
    public void honorsIndependentHeaderAndWorkspaceLimits() throws Exception {
        final byte[] data = png(8, 8);
        assertEquals(8, ImageLoader.load("image/png", data,
            new ImageDecodeLimits(data.length, 8, 64, 1024)).getWidth());
        assertThrows(IOException.class, () -> ImageLoader.load("image/png", data,
            new ImageDecodeLimits(data.length - 1, 8, 64, 1024)));
        assertThrows(IOException.class, () -> ImageLoader.load("image/png", data,
            new ImageDecodeLimits(data.length, 7, 64, 1024)));
        assertThrows(IOException.class, () -> ImageLoader.load("image/png", data,
            new ImageDecodeLimits(data.length, 8, 63, 1024)));
        assertThrows(IOException.class, () -> ImageLoader.load("image/png", data,
            new ImageDecodeLimits(data.length, 8, 64, 1023)));
    }

    /**
     * Metadata arithmetic does not wrap and unreadable input stays a checked IO failure.
     */
    @Test
    public void rejectsOutOfRangeDimensionsAndInvalidImages() throws Exception {
        final ImageDecodeLimits limits = ImageDecodeLimits.defaults();
        assertThrows(IOException.class, () -> limits.checkDimensions(Long.MAX_VALUE, 2));
        assertThrows(IOException.class, () -> limits.checkDimensions(8192, 8192));
        assertThrows(IOException.class, () -> ImageLoader.load("image/png", new byte[0]));
        assertThrows(IOException.class, () -> ImageLoader.load("image/heic", new byte[0]));
    }

    /**
     * @param width ordinary test image width
     * @param height ordinary test image height
     * @return image encoded by the installed PNG writer
     * @throws IOException if encoder IO fails
     */
    private static byte[] png(final int width, final int height) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            final BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            assertTrue(ImageIO.write(image, "PNG", output));
            return output.toByteArray();
        }
    }
}
