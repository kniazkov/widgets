/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOSeekMode;
import openize.io.IOStream;

/**
 * Utility class for loading images from byte arrays.
 * Supports standard formats (via ImageIO) and HEIC format (via the Openize.HEIC library).
 */
public class ImageLoader {
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
            result = ImageIO.read(inputStream);
        }
        return result;
    }

    /**
     * An in-memory {@link IOStream} implementation that wraps a byte array.
     * Provides read-only, seekable access to the underlying data. Used to feed HEIC image data
     * to the Openize.HEIC library.
     */
    public static class ByteArrayIOStream implements IOStream {
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
        public ByteArrayIOStream(byte[] data) {
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
