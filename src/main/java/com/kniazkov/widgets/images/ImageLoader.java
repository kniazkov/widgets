/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOSeekMode;
import openize.io.IOStream;

/**
 * Utility class for loading images from byte arrays.
 * Supports standard formats (via ImageIO) and HEIC format (via the Openize.HEIC library).
 */
public final class ImageLoader {
    /**
     * Private constructor.
     */
    private ImageLoader() {
    }

    /**
     * Loads a {@link BufferedImage} from raw byte data and a specified MIME type.
     * For standard image formats (JPEG, PNG, etc.), uses {@link ImageIO}.
     * For the "image/heic" format, uses the Openize.HEIC library.
     *
     * @param type the MIME type of the image data (e.g., "image/png", "image/heic")
     * @param data the raw byte data of the image
     * @return a {@code BufferedImage} containing the decoded image
     * @throws IOException if an error occurs during reading or decoding the image
     */
    public static BufferedImage load(final String type, final byte[] data) throws IOException {
        return load(type, data, ImageDecodeLimits.defaults());
    }

    /**
     * Checks encoded size and header dimensions before requesting decoded pixels.
     *
     * @param type MIME hint (HEIC/HEIF use the dedicated decoder)
     * @param data encoded image bytes
     * @param limits decoding policy
     * @return decoded image with orientation applied
     * @throws IOException when a limit is exceeded or image cannot be decoded
     */
    public static BufferedImage load(final String type, final byte[] data,
            final ImageDecodeLimits limits) throws IOException {
        Objects.requireNonNull(data, "image bytes");
        Objects.requireNonNull(limits, "limits").checkEncoded(data.length);
        try {
            if ("image/heic".equalsIgnoreCase(type) || "image/heif".equalsIgnoreCase(type)) {
                return loadHeic(data, limits);
            }
            return loadRaster(data, limits);
        } catch (final RuntimeException failure) {
            throw new IOException("Unable to decode image", failure);
        }
    }

    /**
     * Loads a local image under the default policy.
     *
     * @param file local file to load
     * @return decoded image
     * @throws IOException when a limit is exceeded or image cannot be decoded
     */
    public static BufferedImage load(final File file) throws IOException {
        return load(file, ImageDecodeLimits.defaults());
    }

    /**
     * Loads a local file without reading more than the configured byte limit plus one sentinel.
     *
     * @param file local file to load
     * @param limits decoding policy
     * @return decoded image
     * @throws IOException when a limit is exceeded or image cannot be decoded
     */
    public static BufferedImage load(final File file, final ImageDecodeLimits limits)
            throws IOException {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Image is not a regular file");
        }
        final var path = file.toPath();
        limits.checkEncoded(Files.size(path));
        final String name = file.getName().toLowerCase(Locale.ROOT);
        final String type = name.endsWith(".heic") || name.endsWith(".heif") ? "image/heic" : "";
        try (var input = Files.newInputStream(path)) {
            return load(type, input.readNBytes(limits.maxEncodedBytes() + 1), limits);
        }
    }

    /**
     * Reads headers and pixels with the same reader; an unsupported input never decodes.
     *
     * @param data encoded image bytes
     * @param limits decoding policy
     * @return decoded image
     * @throws IOException if metadata or image decoding fails
     */
    private static BufferedImage loadRaster(final byte[] data, final ImageDecodeLimits limits)
            throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            final var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }
            final ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                limits.checkDimensions(reader.getWidth(0), reader.getHeight(0));
                return applyExifOrientation(reader.read(0), orientation(data));
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Checks metadata dimensions including source frames before asking for pixel arrays.
     *
     * @param data encoded image bytes
     * @param limits decoding policy
     * @return decoded image
     * @throws IOException if metadata or workspace is outside the allowed limits
     */
    private static BufferedImage loadHeic(final byte[] data, final ImageDecodeLimits limits)
            throws IOException {
        final ByteArrayIOStream input = new ByteArrayIOStream(data);
        try {
            final HeicImage image = HeicImage.load(input);
            limits.checkDimensions(image.getWidth(), image.getHeight());
            long remaining = limits.workingPixels();
            for (final var frame : image.getAllFrames().values()) {
                remaining -= limits.checkDimensions(frame.getWidth(), frame.getHeight());
                if (remaining < 0) {
                    throw new IOException("HEIC frame workspace limit exceeded");
                }
            }
            final int width = (int) image.getWidth();
            final int height = (int) image.getHeight();
            final int[] pixels = image.getInt32Array(PixelFormat.Argb32);
            final BufferedImage result = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            result.setRGB(0, 0, width, height, pixels, 0, width);
            return result;
        } finally {
            input.close();
        }
    }

    /**
     * @param data encoded image with optional metadata
     * @return EXIF orientation or one if unavailable
     */
    private static int orientation(final byte[] data) {
        try (final ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            final Metadata metadata = ImageMetadataReader.readMetadata(input);
            final ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (final Exception ignored) {
            /*
             * Optional metadata must not prevent a valid raster from loading.
             */
        }
        return 1;
    }

    /**
     * Applies an EXIF orientation transformation to an image.
     * The orientation value corresponds to the TIFF/EXIF 2.31 specification.
     *
     * @param img the original image to transform
     * @param orientation the EXIF orientation tag value (1-8), where 1 means "normal"
     * @return a new {@link BufferedImage} with the orientation applied,
     *  or the original image if orientation is 1 or has an unsupported value
     */
    private static BufferedImage applyExifOrientation(final BufferedImage img,
            final int orientation) {
        if (orientation == 1) {
            return img;
        }
        final int w = img.getWidth();
        final int h = img.getHeight();
        final AffineTransform tx = new AffineTransform();
        int ww = w;
        int hh = h;
        switch (orientation) {
            /*
             * Flip horizontally.
             */
            case 2:
                tx.scale(-1, 1);
                tx.translate(-w, 0);
                break;
            /*
             * Rotate 180 degrees.
             */
            case 3:
                tx.translate(w, h);
                tx.rotate(Math.PI);
                break;
            /*
             * Flip vertically.
             */
            case 4:
                tx.scale(1, -1);
                tx.translate(0, -h);
                break;
            /*
             * Transpose the image.
             */
            case 5:
                tx.rotate(Math.PI / 2);
                tx.scale(1, -1);
                ww = h;
                hh = w;
                tx.translate(0, -w);
                break;
            /*
             * Rotate 90 degrees clockwise.
             */
            case 6:
                tx.translate(h, 0);
                tx.rotate(Math.PI / 2);
                ww = h;
                hh = w;
                break;
            /*
             * Transverse the image.
             */
            case 7:
                tx.rotate(-Math.PI / 2);
                tx.scale(1, -1);
                ww = h;
                hh = w;
                tx.translate(-h, -w);
                break;
            /*
             * Rotate 90 degrees counterclockwise.
             */
            case 8:
                tx.translate(0, w);
                tx.rotate(-Math.PI / 2);
                ww = h;
                hh = w;
                break;
            default:
                return img;
        }

        final BufferedImage out = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        g.setTransform(tx);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    /**
     * An in-memory {@link IOStream} implementation that wraps a byte array.
     * Provides read-only, seekable access to the underlying data. Used to feed HEIC image data
     * to the Openize.HEIC library.
     */
    private static class ByteArrayIOStream implements IOStream {
        /**
         * The underlying byte array containing the data.
         */
        private final byte[] data;

        /**
         * The current read position in the byte array.
         */
        private int position;

        /**
         * Flag indicating whether the stream has been closed.
         */
        private boolean closed;

        /**
         * Creates a new stream that reads from the specified byte array.
         *
         * @param data the byte array to be used as the data source
         */
        ByteArrayIOStream(byte[] data) {
            this.data = data;
            this.position = 0;
            this.closed = false;
        }

        @Override
        public int read(byte[] dst) {
            return read(dst, 0, dst.length);
        }

        @Override
        public int read(byte[] dst, int offset, int count) {
            this.checkClosed();
            if (offset < 0 || count < 0 || offset > dst.length - count) {
                throw new IndexOutOfBoundsException(
                    String.format("Invalid offset/count, offset: %d, count: %d, array length: %d",
                        offset, count, dst.length));
            }
            if (this.position >= this.data.length || count == 0) {
                return -1;
            }
            int bytesToRead = Math.min(count, this.data.length - this.position);
            System.arraycopy(this.data, this.position, dst, offset, bytesToRead);
            this.position += bytesToRead;
            return bytesToRead;
        }

        @Override
        public void write(byte[] data) {
            throw new UnsupportedOperationException("Write operations are not supported");
        }

        @Override
        public void write(byte[] data, int offset, int count) {
            throw new UnsupportedOperationException("Write operations are not supported");
        }

        @Override
        public long setPosition(long newPosition) {
            this.checkClosed();
            if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Position out of valid range: " + newPosition);
            }
            int oldPosition = this.position;
            this.position = (int) Math.min(newPosition, this.data.length);
            return oldPosition;
        }

        @Override
        public long getPosition() {
            this.checkClosed();
            return this.position;
        }

        @Override
        public void seek(long newPosition, IOSeekMode mode) {
            this.checkClosed();
            switch (mode) {
                case BEGIN:
                    if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException(
                            "Invalid seek position from BEGIN: " + newPosition
                        );
                    }
                    this.position = (int) Math.min(newPosition, this.data.length);
                    break;
                case CURRENT:
                    long newPosFromCurrent = this.position + newPosition;
                    if (newPosFromCurrent < 0 || newPosFromCurrent > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException(
                            "Invalid seek position from CURRENT: " + newPosFromCurrent
                        );
                    }
                    this.position = (int) Math.max(
                        0,
                        Math.min(newPosFromCurrent, this.data.length)
                    );
                    break;
                case END:
                    long newPosFromEnd = this.data.length + newPosition;
                    if (newPosFromEnd < 0 || newPosFromEnd > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException(
                            "Invalid seek position from END: " + newPosFromEnd
                        );
                    }
                    position = (int) Math.max(0, Math.min(newPosFromEnd, this.data.length));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown seek mode: " + mode);
            }
        }

        @Override
        public long getLength() {
            checkClosed();
            return data.length;
        }

        @Override
        public void setLength(long newLength) {
            throw new UnsupportedOperationException("setLength operation is not supported");
        }

        @Override
        public void close() {
            closed = true;
        }

        /**
         * Throws an {@link IllegalStateException} if the stream has been closed.
         */
        private void checkClosed() {
            if (closed) {
                throw new IllegalStateException("Stream is closed");
            }
        }
    }
}
