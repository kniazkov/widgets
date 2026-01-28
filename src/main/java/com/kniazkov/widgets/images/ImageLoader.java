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
import javax.imageio.ImageIO;
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
        final BufferedImage result;
        if (type.equals("image/heic")) {
            final ByteArrayIOStream inputStream = new ByteArrayIOStream(data);
            final HeicImage image = HeicImage.load(inputStream);
            final int width = (int)image.getWidth();
            final int height = (int)image.getHeight();
            final int[] pixels = image.getInt32Array(PixelFormat.Argb32);
            result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            result.setRGB(0, 0, width, height, pixels, 0, width);
        } else {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            final BufferedImage image = ImageIO.read(inputStream);
            int orientation = 1;
            try (final ByteArrayInputStream metaIn = new ByteArrayInputStream(data)) {
                final Metadata metadata = ImageMetadataReader.readMetadata(metaIn);
                final ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(
                    ExifIFD0Directory.class
                );
                if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            } catch (final Exception ignore) {
            }
            result = applyExifOrientation(image, orientation);
        }
        return result;
    }

    /**
     * Loads a {@link BufferedImage} from a file, preserving transparency if present.
     * Automatically detects the MIME type based on the file extension.
     *
     * @param file the file to load the image from
     * @return a {@code BufferedImage} containing the decoded image with preserved transparency
     * @throws IOException if an error occurs during reading or decoding the image
     * @throws IllegalArgumentException if the file is null, doesn't exist, or has an
     *  unsupported format
     */
    public static BufferedImage load(final File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getPath());
        }
        final byte[] data = Files.readAllBytes(file.toPath());
        final String fileName = file.getName().toLowerCase();
        final String type;
        if (fileName.endsWith(".heic") || fileName.endsWith(".heif")) {
            type = "image/heic";
        } else if (fileName.endsWith(".png")) {
            type = "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            type = "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            type = "image/gif";
        } else if (fileName.endsWith(".bmp")) {
            type = "image/bmp";
        } else if (fileName.endsWith(".tif") || fileName.endsWith(".tiff")) {
            type = "image/tiff";
        } else if (fileName.endsWith(".webp")) {
            type = "image/webp";
        } else {
            try {
                final String detectedType = ImageIO.getImageReaders(
                    ImageIO.createImageInputStream(new ByteArrayInputStream(data))
                ).next().getFormatName().toLowerCase();
                if (detectedType.contains("heic") || detectedType.contains("heif")) {
                    type = "image/heic";
                } else {
                    type = "image/" + detectedType;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "Unsupported image format for file: " + file.getPath(), e
                );
            }
        }
        return load(type, data);
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
        int ww = w, hh = h;
        switch (orientation) {
            case 2: // flip X
                tx.scale(-1, 1);
                tx.translate(-w, 0);
                break;
            case 3: // 180
                tx.translate(w, h);
                tx.rotate(Math.PI);
                break;
            case 4: // flip Y
                tx.scale(1, -1);
                tx.translate(0, -h);
                break;
            case 5: // transpose
                tx.rotate(Math.PI / 2);
                tx.scale(1, -1);
                ww = h; hh = w;
                tx.translate(0, -w);
                break;
            case 6: // 90 CW
                tx.translate(h, 0);
                tx.rotate(Math.PI / 2);
                ww = h; hh = w;
                break;
            case 7: // transverse
                tx.rotate(-Math.PI / 2);
                tx.scale(1, -1);
                ww = h; hh = w;
                tx.translate(-h, -w);
                break;
            case 8: // 90 CCW
                tx.translate(0, w);
                tx.rotate(-Math.PI / 2);
                ww = h; hh = w;
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
