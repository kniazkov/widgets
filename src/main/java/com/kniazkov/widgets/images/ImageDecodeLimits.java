/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.io.IOException;

/**
 * Independent encoded input, dimension and estimated decoded workspace caps.
 *
 * @param maxEncodedBytes maximum encoded input size
 * @param maxDimension maximum width or height
 * @param maxPixels maximum pixels in a decoded image
 * @param maxWorkingBytes conservative pixel workspace budget, not a JVM allocation guarantee
 */
public record ImageDecodeLimits(int maxEncodedBytes, int maxDimension, long maxPixels,
        long maxWorkingBytes) {
    /**
     * Validates positive limits and leaves room for one overflow detection byte.
     */
    public ImageDecodeLimits {
        if (maxEncodedBytes < 1 || maxEncodedBytes == Integer.MAX_VALUE || maxDimension < 1
                || maxPixels < 1 || maxWorkingBytes < 1) {
            throw new IllegalArgumentException("Invalid image decoding limits");
        }
    }

    /**
     * @return bounded defaults permitting common 12 MP camera images
     */
    public static ImageDecodeLimits defaults() {
        return new ImageDecodeLimits(32 * 1024 * 1024, 8192, 16_000_000, 256L * 1024 * 1024);
    }

    /**
     * Checks the input before allocating intermediate decoder resources.
     *
     * @param encoded number of input bytes
     * @throws IOException if the input exceeds the encoded budget
     */
    void checkEncoded(final long encoded) throws IOException {
        if (encoded > this.maxEncodedBytes) {
            throw new IOException("Encoded image exceeds its size limit");
        }
    }

    /**
     * Checks frame dimensions and estimated temporary raster workspace before decoding.
     *
     * @param width encoded width
     * @param height encoded height
     * @return pixels in the frame
     * @throws IOException if dimensions, pixels or workspace exceed limits
     */
    long checkDimensions(final long width, final long height) throws IOException {
        if (width < 1 || height < 1 || width > this.maxDimension || height > this.maxDimension) {
            throw new IOException("Image dimensions exceed their limit");
        }
        final long pixels = width * height;
        if (pixels > this.maxPixels || pixels > this.workingPixels()) {
            throw new IOException("Image pixel or decoded workspace limit exceeded");
        }
        return pixels;
    }

    /**
     * @return estimated pixels fitting in decoder, conversion and orientation work buffers
     */
    long workingPixels() {
        return this.maxWorkingBytes / 16;
    }
}
