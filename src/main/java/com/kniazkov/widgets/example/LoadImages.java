/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.images.BufferedImageSource;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.images.CircleProgressBarCreator;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.ImageWidget;
import com.kniazkov.widgets.view.Section;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import openize.io.IOStream;
import openize.io.IOSeekMode;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;

/**
 * A demonstration program that allows uploading and displaying multiple images.
 * <p>
 * This example creates a file upload widget configured to accept multiple image files.
 * Uploaded images are automatically cropped to square aspect ratios and resized to
 * 300x300 pixels with high-quality rendering, then displayed in a gallery layout.
 *
 * <b>How to use</b>
 * <ol>
 *   <li>Run the program;</li>
 *   <li>
 *     Open your browser and go to
 *     <a href="http://localhost:8000">http://localhost:8000</a>.
 *   </li>
 * </ol>
 */
public class LoadImages {
    /**
     * Entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        final Set<Listener<Integer>> listeners = new HashSet<>();
        final Page page = (root, parameters) -> {
            final Section main = new Section();
            root.add(main);

            final Section images = new Section();
            root.add(images);

            final CircleProgressBarCreator progress = new CircleProgressBarCreator(
                300,
                300
            );

            final FileLoader loader = new FileLoader("Click me");
            main.add(loader);
            loader.setMultipleInputFlag(true);
            loader.acceptImagesOnly();
            loader.onSelect(descriptor -> {
                final ImageWidget widget = new ImageWidget(
                    progress.getImageSource(0)
                );
                images.add(widget);
                widget.setWidth(300);
                widget.setHeight(300);
                widget.setBorderWidth(1);
                widget.setBorderColor(Color.BLACK);
                final Listener<Integer> listener =
                    percent -> widget.setSource(progress.getImageSource(percent));
                listeners.add(listener);
                descriptor.getLoadingPercentageModel().addListener(listener);
                descriptor.onLoad(file-> {
                    try {
                        System.out.println("Type: " + file.getType());
                        final BufferedImage originalImage;
                        if (file.getType().equals("image/heic")) {
                            final ByteArrayIOStream inputStream = new ByteArrayIOStream(
                                file.getContent(),
                                0
                            );
                            final HeicImage image = HeicImage.load(inputStream);
                            System.out.println("Loaded image");
                            final int width = (int)image.getWidth();
                            final int height = (int)image.getHeight();
                            final int[] pixels = image.getInt32Array(PixelFormat.Argb32);
                            System.out.println("Get array");
                            originalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                            final int[] imageBuffer = ((DataBufferInt) originalImage.getRaster().getDataBuffer()).getData();
                            System.out.println("Get dst buffer");
                            System.arraycopy(pixels, 0, imageBuffer, 0, pixels.length);
                            System.out.println("Data copied");
                        } else {
                            final ByteArrayInputStream inputStream = new ByteArrayInputStream(
                                file.getContent()
                            );
                            originalImage = ImageIO.read(inputStream);
                        }
                        int minSize = Math.min(originalImage.getWidth(), originalImage.getHeight());
                        int x = (originalImage.getWidth() - minSize) / 2;
                        int y = (originalImage.getHeight() - minSize) / 2;
                        final BufferedImage croppedImage = originalImage.getSubimage(
                            x,
                            y,
                            minSize,
                            minSize
                        );
                        final BufferedImage resizedImage = new BufferedImage(
                            300,
                            300,
                            BufferedImage.TYPE_INT_ARGB
                        );
                        Graphics2D g2d = resizedImage.createGraphics();
                        g2d.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                        ;
                        g2d.setRenderingHint(
                            RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY
                        );
                        g2d.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON
                        );
                        g2d.drawImage(
                            croppedImage,
                            0,
                            0,
                            300,
                            300,
                            null
                        );
                        g2d.dispose();
                        widget.setSource(new BufferedImageSource(resizedImage));
                    } catch (final IOException ignored) {
                        images.remove(widget);
                    } finally {
                        listeners.remove(listener);
                    }
                });
            });
        };

        final Application application = new Application(page);
        final Options options = new Options();
        Server.start(application, options);
    }

    /**
     * Обертка для работы с byte[] как с потоком данных (только для чтения).
     */
    public static class ByteArrayIOStream implements IOStream {
        private final byte[] data;
        private int position;
        private boolean closed;

        /**
         * Создает новый поток для чтения из указанного массива байт.
         * 
         * @param data исходный массив байт
         * @throws NullPointerException если data равен null
         */
        public ByteArrayIOStream(byte[] data) {
            this.data = Objects.requireNonNull(data, "Data array cannot be null");
            this.position = 0;
            this.closed = false;
        }

        /**
         * Создает новый поток для чтения из указанного массива байт
         * с начальной позицией.
         * 
         * @param data исходный массив байт
         * @param startPosition начальная позиция в потоке
         * @throws NullPointerException если data равен null
         * @throws IllegalArgumentException если startPosition некорректна
         */
        public ByteArrayIOStream(byte[] data, int startPosition) {
            this(data);
            if (startPosition < 0 || startPosition > data.length) {
                throw new IllegalArgumentException("Invalid start position: " + startPosition);
            }
            this.position = startPosition;
        }

        @Override
        public int read(byte[] dst) {
            checkClosed();
            Objects.requireNonNull(dst, "Destination array cannot be null");
            return read(dst, 0, dst.length);
        }

        @Override
        public int read(byte[] dst, int offset, int count) {
            checkClosed();
            Objects.requireNonNull(dst, "Destination array cannot be null");
            
            if (offset < 0 || count < 0 || offset > dst.length - count) {
                throw new IndexOutOfBoundsException(
                    String.format("Invalid offset/count: offset=%d, count=%d, array length=%d", 
                    offset, count, dst.length));
            }

            if (position >= data.length || count == 0) {
                return -1; // Конец потока
            }

            int bytesToRead = Math.min(count, data.length - position);
            System.arraycopy(data, position, dst, offset, bytesToRead);
            position += bytesToRead;

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
            checkClosed();
            if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Position out of valid range: " + newPosition);
            }
            
            int oldPosition = position;
            position = (int) Math.min(newPosition, data.length);
            return oldPosition;
        }

        @Override
        public long getPosition() {
            checkClosed();
            return position;
        }

        @Override
        public void seek(long newPosition, IOSeekMode mode) {
            checkClosed();
            
            switch (mode) {
                case BEGIN:
                    if (newPosition < 0) {
                        throw new IllegalArgumentException("Cannot seek to negative position from BEGIN");
                    }
                    position = (int) Math.min(newPosition, data.length);
                    break;
                    
                case CURRENT:
                    long newPosFromCurrent = position + newPosition;
                    if (newPosFromCurrent < 0 || newPosFromCurrent > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Invalid seek position from CURRENT: " + newPosFromCurrent);
                    }
                    position = (int) Math.max(0, Math.min(newPosFromCurrent, data.length));
                    break;
                    
                case END:
                    long newPosFromEnd = data.length + newPosition;
                    if (newPosFromEnd < 0 || newPosFromEnd > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Invalid seek position from END: " + newPosFromEnd);
                    }
                    position = (int) Math.max(0, Math.min(newPosFromEnd, data.length));
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
         * Проверяет, закрыт ли поток.
         * 
         * @throws IOException если поток закрыт
         */
        private void checkClosed() {
            if (closed) {
                throw new IllegalStateException("Stream is closed");
            }
        }

        /**
         * Возвращает исходный массив байт (без копирования).
         * 
         * @return исходный массив байт
         */
        public byte[] getData() {
            return data;
        }

        /**
         * Проверяет, достигнут ли конец потока.
         * 
         * @return true если достигнут конец потока
         */
        public boolean isEOF() {
            return position >= data.length;
        }

        /**
         * Возвращает количество байт, доступных для чтения.
         * 
         * @return количество доступных байт
         */
        public int available() {
            return Math.max(0, data.length - position);
        }

        /**
         * Сбрасывает позицию чтения в начало потока.
         */
        public void reset() {
            checkClosed();
            position = 0;
        }
    }
}
