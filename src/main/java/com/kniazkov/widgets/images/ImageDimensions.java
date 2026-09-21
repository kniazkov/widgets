/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.IntrinsicSize;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads original image dimensions without decoding raster pixels or rasterizing SVG.
 * Raster formats are detected by installed ImageIO readers; SVG is selected by filename.
 * Returned raster dimensions describe the encoded first image, before EXIF orientation.
 */
public final class ImageDimensions {
    /**
     * Positive SVG lengths with an optional absolute CSS unit.
     */
    private static final Pattern LENGTH = Pattern.compile(
        "([+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)"
            + "\\s*(px|pt|pc|in|cm|mm|q)?");

    /**
     * Static utility only.
     */
    private ImageDimensions() {
    }

    /**
     * Reads dimensions from already loaded bytes. SVG filenames are case insensitive;
     * other filename extensions do not override raster format detection by ImageIO.
     *
     * @param data encoded image bytes
     * @param name filename, not a URL or MIME type; used to recognize .svg
     * @return positive original dimensions
     * @throws IOException if the format is unsupported, metadata is invalid or SVG is unsafe
     */
    public static IntrinsicSize read(final byte[] data, final String name) throws IOException {
        Objects.requireNonNull(data, "data");
        if (isSvg(name)) {
            try (InputStream input = new ByteArrayInputStream(data)) {
                return readSvg(input);
            }
        }
        try (ImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(data))) {
            return readRaster(input);
        }
    }

    /**
     * Reads dimensions directly from a file. Raster access is seekable and does not load
     * the entire file into a byte array. SVG is parsed from a stream as an XML document.
     * All streams and readers are closed before this method returns.
     *
     * @param file local image path; SVG is selected by its filename extension
     * @return positive original dimensions
     * @throws IOException if the file cannot be read or its dimensions cannot be determined
     */
    public static IntrinsicSize read(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        if (!Files.isRegularFile(file)) {
            throw new IOException("Image is not a regular file: " + file);
        }
        if (isSvg(file.getFileName().toString())) {
            try (InputStream input = Files.newInputStream(file)) {
                return readSvg(input);
            }
        }
        try (ImageInputStream input = new FileImageInputStream(file.toFile())) {
            return readRaster(input);
        }
    }

    /**
     * Convenience overload for existing File-based callers.
     * @param file local image file
     * @return positive original dimensions
     * @throws IOException if the file cannot be read or its dimensions cannot be determined
     */
    public static IntrinsicSize read(final File file) throws IOException {
        return read(Objects.requireNonNull(file, "file").toPath());
    }

    /**
     * @param name filename
     * @return whether the filename selects the SVG parser
     */
    private static boolean isSvg(final String name) {
        return Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT).endsWith(".svg");
    }

    /**
     * Reads header dimensions using an ImageIO reader without calling read().
     * @param input seekable encoded data
     * @return first-image dimensions
     * @throws IOException if no reader exists or the header is invalid
     */
    private static IntrinsicSize readRaster(final ImageInputStream input) throws IOException {
        final var readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            throw new IOException("Unsupported image format");
        }
        final ImageReader reader = readers.next();
        try {
            reader.setInput(input);
            return new IntrinsicSize(dimension(reader.getWidth(0)), dimension(reader.getHeight(0)));
        } catch (final IllegalArgumentException error) {
            throw new IOException("Invalid image dimensions", error);
        } finally {
            reader.dispose();
        }
    }

    /**
     * Parses SVG with DTDs, external entities, schemas and XInclude disabled.
     * @param input SVG document stream
     * @return pixel dimensions rounded up to positive integers
     * @throws IOException if XML or dimensions are invalid or secure parsing is unavailable
     */
    private static IntrinsicSize readSvg(final InputStream input) throws IOException {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            final var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(final SAXParseException error) throws SAXException {
                    throw error;
                }

                @Override
                public void fatalError(final SAXParseException error) throws SAXException {
                    throw error;
                }
            });
            final Element svg = builder.parse(input).getDocumentElement();
            if (!"svg".equals(svg.getLocalName()) || !(svg.getNamespaceURI() == null
                    || "http://www.w3.org/2000/svg".equals(svg.getNamespaceURI()))) {
                throw new IOException("Expected an SVG root element");
            }
            double width = pixels(svg.getAttribute("width"));
            double height = pixels(svg.getAttribute("height"));
            final String[] viewBox = svg.getAttribute("viewBox").trim().split("[\\s,]+");
            if (viewBox.length == 4 && (width <= 0 || height <= 0)) {
                final double x = Double.parseDouble(viewBox[0]);
                final double y = Double.parseDouble(viewBox[1]);
                final double vw = Double.parseDouble(viewBox[2]);
                final double vh = Double.parseDouble(viewBox[3]);
                if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(vw)
                        && Double.isFinite(vh) && vw > 0 && vh > 0) {
                    if (width > 0) {
                        height = width * vh / vw;
                    } else if (height > 0) {
                        width = height * vw / vh;
                    } else {
                        width = vw;
                        height = vh;
                    }
                }
            }
            return new IntrinsicSize(dimension(width), dimension(height));
        } catch (final ParserConfigurationException | SAXException | IllegalArgumentException
                | UnsupportedOperationException error) {
            throw new IOException("Cannot read SVG dimensions safely", error);
        }
    }

    /**
     * Resolves an absolute SVG length at 96 CSS pixels per inch.
     * @param text attribute value
     * @return pixels, or zero if an external viewport would be needed
     */
    private static double pixels(final String text) {
        final var matcher = LENGTH.matcher(text.trim());
        if (!matcher.matches()) {
            return 0;
        }
        final double value = Double.parseDouble(matcher.group(1));
        final String unit = matcher.group(2);
        return value * switch (unit == null ? "px" : unit) {
            case "pt" -> 96.0 / 72;
            case "pc" -> 16;
            case "in" -> 96;
            case "cm" -> 96 / 2.54;
            case "mm" -> 96 / 25.4;
            case "q" -> 96 / 101.6;
            default -> 1;
        };
    }

    /**
     * @param value positive finite pixel dimension
     * @return dimension rounded up
     * @throws IOException if no positive int can represent the dimension
     */
    private static int dimension(final double value) throws IOException {
        if (!Double.isFinite(value) || value <= 0 || value > Integer.MAX_VALUE) {
            throw new IOException("Cannot determine positive image dimensions");
        }
        return (int) Math.ceil(value);
    }
}
