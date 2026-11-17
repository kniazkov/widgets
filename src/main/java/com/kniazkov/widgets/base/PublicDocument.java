/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Immutable public content shared by initial HTML and the interactive page.
 * Supports semantic headings, paragraphs, links and images with reserved dimensions.
 * No raw HTML, styles, handlers, forms or session-dependent widgets are accepted.
 *
 * @param title page title and social title
 * @param description page description and social description
 * @param canonical absolute HTTP(S) canonical URL, or empty
 * @param image absolute HTTP(S) social image URL, or empty
 * @param blocks ordered public blocks
 */
public record PublicDocument(String title, String description, String canonical,
        String image, List<Block> blocks) {
    /**
     * Validates URLs and copies the content list.
     */
    public PublicDocument {
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        canonical = url(canonical, true, true);
        image = url(image, true, true);
        blocks = List.copyOf(blocks);
    }

    /**
     * A single safe semantic block. Only the factory methods construct blocks.
     */
    public static final class Block {
        /**
         * Semantic HTML tag.
         */
        private final String tag;
        /**
         * Plain text or image alternative text.
         */
        private final String text;
        /**
         * Validated link or image URL.
         */
        private final String address;
        /**
         * Reserved image width.
         */
        private final int width;
        /**
         * Reserved image height.
         */
        private final int height;

        /**
         * @param tag safe tag
         * @param text plain text
         * @param address safe URL
         * @param width image width
         * @param height image height
         */
        private Block(final String tag, final String text, final String address,
                final int width, final int height) {
            this.tag = tag;
            this.text = Objects.requireNonNull(text);
            this.address = address;
            this.width = width;
            this.height = height;
        }

        /**
         * @return JSON data for native DOM construction, never executable HTML
         */
        private JsonObject json() {
            final JsonObject result = new JsonObject();
            result.addString("tag", this.tag);
            result.addString("text", this.text);
            result.addString("url", this.address);
            result.addNumber("width", this.width);
            result.addNumber("height", this.height);
            return result;
        }

        /**
         * @return escaped static HTML with the same structure as the native DOM renderer
         */
        private String html() {
            if ("img".equals(this.tag)) {
                return "<img src=\"" + escape(this.address) + "\" alt=\"" + escape(this.text)
                    + "\" width=\"" + this.width + "\" height=\"" + this.height + "\">";
            }
            if ("a".equals(this.tag)) {
                return "<p><a href=\"" + escape(this.address) + "\">" + escape(this.text)
                    + "</a></p>";
            }
            return "<" + this.tag + ">" + escape(this.text) + "</" + this.tag + ">";
        }
    }

    /**
     * @param level heading level from one to six
     * @param text plain heading text
     * @return heading block
     */
    public static Block heading(final int level, final String text) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Heading level must be between 1 and 6");
        }
        return new Block("h" + level, text, "", 0, 0);
    }

    /**
     * @param text plain paragraph text, never markup
     * @return paragraph block
     */
    public static Block paragraph(final String text) {
        return new Block("p", text, "", 0, 0);
    }

    /**
     * @param text link text
     * @param href root-relative or absolute HTTP(S) URL
     * @return ordinary navigable link, without a server event handler
     */
    public static Block link(final String text, final String href) {
        return new Block("a", text, url(href, false, false), 0, 0);
    }

    /**
     * @param source root-relative or absolute HTTP(S) image URL
     * @param alternative alternative text
     * @param width intrinsic width in pixels
     * @param height intrinsic height in pixels
     * @return image with reserved aspect ratio
     */
    public static Block image(final String source, final String alternative,
            final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        return new Block("img", alternative, url(source, false, false), width, height);
    }

    /**
     * @return escaped content; the caller owns the surrounding page container
     */
    String html() {
        final StringBuilder result = new StringBuilder("<article class=\"widgets-public\">");
        this.blocks.forEach(block -> result.append(block.html()));
        return result.append("</article>").toString();
    }

    /**
     * @return public document data without transport credentials or widget IDs
     */
    JsonObject json() {
        final JsonObject result = new JsonObject();
        result.addString("title", this.title);
        result.addString("description", this.description);
        result.addString("canonical", this.canonical);
        result.addString("image", this.image);
        final JsonArray content = new JsonArray();
        this.blocks.forEach(block -> content.addElement(block.json()));
        result.addElement("blocks", content);
        return result;
    }

    /**
     * @return page-specific head elements, always escaped
     */
    String head() {
        final StringBuilder result = new StringBuilder("<title>");
        result.append(escape(this.title)).append("</title>\n");
        result.append("<meta name=\"description\" content=\"")
            .append(escape(this.description)).append("\">\n");
        if (!this.canonical.isEmpty()) {
            result.append("<link rel=\"canonical\" href=\"")
                .append(escape(this.canonical)).append("\">\n");
        }
        final String[] names = {"og:title", "og:description", "og:url", "og:image"};
        final String[] values = {this.title, this.description, this.canonical, this.image};
        for (int index = 0; index < names.length; index++) {
            if (!values[index].isEmpty()) {
                result.append("<meta property=\"").append(names[index]).append("\" content=\"")
                    .append(escape(values[index])).append("\">\n");
            }
        }
        return result.toString();
    }

    /**
     * @param value plain text
     * @return text safe in both HTML attributes and text nodes
     */
    private static String escape(final String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * @param value URL
     * @param absolute whether root-relative URLs are forbidden
     * @param optional whether an empty value is allowed
     * @return validated URL
     */
    private static String url(final String value, final boolean absolute, final boolean optional) {
        Objects.requireNonNull(value);
        if (optional && value.isEmpty()) {
            return value;
        }
        final URI parsed = URI.create(value);
        final boolean relative = !absolute && value.startsWith("/") && !value.startsWith("//")
            && parsed.getRawAuthority() == null;
        final boolean http = ("https".equalsIgnoreCase(parsed.getScheme())
            || "http".equalsIgnoreCase(parsed.getScheme())) && parsed.getHost() != null
            && parsed.getUserInfo() == null;
        if ((!relative && !http) || value.contains("\\")
                || value.chars().anyMatch(character -> character <= 32 || character == 127)) {
            throw new IllegalArgumentException("Expected a safe HTTP(S) URL");
        }
        return value;
    }
}
