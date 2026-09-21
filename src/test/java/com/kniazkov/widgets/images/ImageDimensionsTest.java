/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.images;

import com.kniazkov.widgets.common.IntrinsicSize;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the same dimension contract for loaded bytes and unopened files.
 */
public class ImageDimensionsTest {
    /**
     * Files owned by each test.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * Every raster entry point detects the encoded format rather than trusting its extension.
     */
    @Test
    public void readsRasterBytesPathsAndFiles() throws Exception {
        for (final String format : new String[]{"png", "jpeg", "gif", "bmp"}) {
            final byte[] bytes = raster(format);
            final var file = this.folder.newFile(format + ".data").toPath();
            Files.write(file, bytes);
            final IntrinsicSize expected = new IntrinsicSize(350, 100);
            assertEquals(expected, ImageDimensions.read(bytes, "unknown.extension"));
            assertEquals(expected, ImageDimensions.read(file));
            assertEquals(expected, ImageDimensions.read(file.toFile()));
            Files.delete(file);
        }
    }

    /**
     * Pixel payload is unnecessary: a PNG IHDR alone is enough to read width and height.
     */
    @Test
    public void doesNotDecodeRasterPixels() throws Exception {
        final byte[] header = Arrays.copyOf(raster("png"), 33);
        final var file = this.folder.newFile("header.png").toPath();
        Files.write(file, header);
        assertEquals(new IntrinsicSize(350, 100), ImageDimensions.read(header, "header.png"));
        assertEquals(new IntrinsicSize(350, 100), ImageDimensions.read(file));
    }

    /**
     * Explicit lengths, partial lengths and viewport-independent viewBox fallbacks.
     */
    @Test
    public void readsSvgDimensions() throws Exception {
        final String[][] cases = {
            {"width='350' height='100px'", "350 100"},
            {"viewBox='-10 -20 350 100'", "350 100"},
            {"width='100%' height='auto' viewBox='0,0,350,100'", "350 100"},
            {"width='700' viewBox='0 0 350 100'", "700 200"},
            {"height='200' viewBox='0 0 350 100'", "700 200"},
            {"width='3.5e2' height='100'", "350 100"},
            {"width='0.5' height='1.1'", "1 2"},
            {"width='1in' height='36pt'", "96 48"},
            {"width='6pc' height='12.7mm'", "96 48"},
            {"width='2.54cm' height='50.8q'", "96 48"}
        };
        for (final String[] item : cases) {
            final byte[] data = svg(item[0]);
            final var file = Files.createTempFile(this.folder.getRoot().toPath(), "image-", ".SVG");
            Files.write(file, data);
            assertEquals(item[0], item[1], ImageDimensions.read(data, "image.SVG").toString());
            assertEquals(item[1], ImageDimensions.read(file).toString());
            assertEquals(item[1], ImageDimensions.read(file.toFile()).toString());
        }
    }

    /**
     * Common XML encodings and namespace forms remain accepted without network access.
     */
    @Test
    public void supportsSvgNamespacesAndEncoding() throws Exception {
        for (final String xml : new String[]{
            "<svg width='350' height='100'/>",
            "<?xml version='1.0' encoding='UTF-8'?><!-- comment -->"
                + "<s:svg xmlns:s='http://www.w3.org/2000/svg' viewBox='0 0 350 100'/>"
        }) {
            assertEquals(new IntrinsicSize(350, 100), ImageDimensions.read(
                xml.getBytes(StandardCharsets.UTF_8), "a.svg"));
        }
        final String utf16 = "<?xml version='1.0' encoding='UTF-16'?>"
            + "<svg xmlns='http://www.w3.org/2000/svg' width='350' height='100'/>";
        assertEquals(new IntrinsicSize(350, 100), ImageDimensions.read(
            utf16.getBytes(StandardCharsets.UTF_16), "a.svg"));
    }

    /**
     * Invalid dimensions are checked failures, never a fabricated default size or NONE.
     */
    @Test
    public void rejectsInvalidSvgDimensions() {
        for (final String attrs : new String[]{"", "width='0' height='-1'",
            "width='100%' height='50%'", "viewBox='0 0 0 100'", "viewBox='0 0 -1 100'",
            "viewBox='NaN 0 350 100'", "viewBox='0 0 NaN 100'", "viewBox='bad numbers'",
            "viewBox='0 0 wrong 100'", "width='1e999' height='100'",
            "width='2147483648' height='100'"}) {
            assertThrows(attrs, IOException.class, () -> ImageDimensions.read(svg(attrs), "a.svg"));
        }
    }

    /**
     * DTDs and entities are rejected for both file and byte entry points.
     */
    @Test
    public void rejectsUnsafeAndMalformedXml() throws Exception {
        final var secret = this.folder.newFile("secret.txt").toPath();
        Files.writeString(secret, "350");
        final var file = this.folder.newFile("bad.svg").toPath();
        for (final String xml : new String[]{
            "<!DOCTYPE svg [<!ENTITY x SYSTEM '" + secret.toUri() + "'>]>"
                + "<svg width='350' height='100'>&x;</svg>",
            "<!DOCTYPE svg SYSTEM 'http://127.0.0.1:1/external.dtd'>"
                + "<svg width='350' height='100'/>",
            "<!DOCTYPE svg [<!ENTITY x '350'>]><svg width='&x;' height='100'/>",
            "<html width='350' height='100'/>",
            "<svg xmlns='urn:not-svg' width='350' height='100'/>",
            "<svg width='350' height='100'>"
        }) {
            final byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
            Files.write(file, bytes);
            assertThrows(IOException.class, () -> ImageDimensions.read(bytes, "a.svg"));
            assertThrows(IOException.class, () -> ImageDimensions.read(file));
        }
    }

    /**
     * Missing files, directories, unknown raster formats and truncated headers fail predictably.
     */
    @Test
    public void reportsReadFailures() throws Exception {
        assertThrows(IOException.class, () -> ImageDimensions.read(
            this.folder.getRoot().toPath().resolve("missing.png")));
        assertThrows(IOException.class, () -> ImageDimensions.read(this.folder.getRoot()));
        for (final byte[] bytes : new byte[][]{new byte[0], new byte[]{1, 2, 3},
                Arrays.copyOf(raster("png"), 12)}) {
            assertThrows(IOException.class, () -> ImageDimensions.read(bytes, "file.png"));
        }
    }

    /**
     * @param attributes attributes for a complete SVG root
     * @return UTF-8 SVG bytes
     */
    private static byte[] svg(final String attributes) {
        return ("<svg xmlns='http://www.w3.org/2000/svg' " + attributes + "/>")
            .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @param format installed raster writer format
     * @return encoded image bytes
     * @throws IOException if encoding fails
     */
    private static byte[] raster(final String format) throws IOException {
        final var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(350, 100, BufferedImage.TYPE_INT_RGB),
            format, output));
        return output.toByteArray();
    }
}
