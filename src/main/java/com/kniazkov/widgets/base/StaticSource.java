/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An explicitly public directory or classpath subtree mounted at a URL prefix.
 * Instances are immutable. Source directories must be controlled by trusted code.
 */
public final class StaticSource {
    /**
     * Canonical URL prefix, with a trailing slash.
     */
    private final String prefix;
    /**
     * Filesystem root, or null for classpath sources.
     */
    private final Path directory;
    /**
     * Class defining the resource lookup context, or null for directories.
     */
    private final Class<?> anchor;
    /**
     * Absolute classpath subtree, or null for directories.
     */
    private final String resourceRoot;

    /**
     * Creates one validated source.
     */
    private StaticSource(final String prefix, final Path directory,
            final Class<?> anchor, final String resourceRoot) {
        this.prefix = canonicalRoot(prefix);
        final String first = this.prefix.substring(1).split("/")[0];
        if (first.equals("scripts") || first.equals("fonts")
                || first.equals("index.html") || first.equals("style.css")) {
            throw new IllegalArgumentException("Framework URL prefix is reserved: " + prefix);
        }
        this.directory = directory;
        this.anchor = anchor;
        this.resourceRoot = resourceRoot;
    }

    /**
     * Publishes a directory. Relative directories are resolved when registered.
     * The directory may be created later; missing files return 404.
     *
     * @param prefix absolute non-root URL prefix, for example /media/
     * @param directory directory containing only public files
     * @return immutable source
     */
    public static StaticSource directory(final String prefix, final Path directory) {
        final Path root = Objects.requireNonNull(directory, "directory")
            .toAbsolutePath().normalize();
        return new StaticSource(prefix, root, null, null);
    }

    /**
     * Publishes an absolute, non-root classpath subtree from an exploded directory or JAR.
     * No lookup is performed outside that explicitly public subtree.
     *
     * @param prefix absolute non-root URL prefix
     * @param anchor class whose resource lookup context is used
     * @param resourceRoot absolute public resource subtree, for example /public/icons/
     * @return immutable source
     */
    public static StaticSource classpath(final String prefix, final Class<?> anchor,
            final String resourceRoot) {
        return new StaticSource(prefix, null, Objects.requireNonNull(anchor, "anchor"),
            canonicalRoot(resourceRoot));
    }

    /**
     * Returns the normalized public prefix.
     *
     * @return URL prefix ending in a slash
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Returns a root-relative URL pinned to the current file contents. Generate it again
     * after replacing the file. The same source must be registered in server options.
     * An overlapping, more specific source still takes precedence when serving the URL.
     *
     * @param relativePath decoded filename relative to this source, without query or fragment
     * @return percent-encoded URL with a content version suitable for long-lived browser caching
     * @throws IOException if the file cannot be read
     * @throws SecurityException if the path escapes this source
     */
    public String versionedUrl(final String relativePath) throws IOException {
        final String path = this.prefix + Objects.requireNonNull(relativePath, "relativePath");
        final String hash = contentHash(this.read(path));
        try {
            return new URI(null, null, path, "widgets-version=" + hash, null).toASCIIString();
        } catch (final URISyntaxException error) {
            throw new IllegalArgumentException("Invalid static resource path", error);
        }
    }

    /**
     * Computes the shared URL version and ETag payload from the actual response bytes.
     * @param data resource bytes
     * @return lowercase SHA-256
     */
    static String contentHash(final byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (final NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by Java", error);
        }
    }

    /**
     * Matches complete URL path segments, including the bare mount point.
     */
    boolean matches(final String address) {
        return address.startsWith(this.prefix)
            || address.equals(this.prefix.substring(0, this.prefix.length() - 1));
    }

    /**
     * Reads a file from this source, never falling back to another source.
     */
    byte[] read(final String address) throws IOException {
        if (!matches(address) || address.length() <= this.prefix.length()) {
            throw new NoSuchFileException(address);
        }
        final String relative = address.substring(this.prefix.length());
        requireRelative(relative);
        if (this.directory != null) {
            return readDirectory(this.directory, relative);
        }
        final String name = this.resourceRoot + relative;
        final URL resource = this.anchor.getResource(name);
        if (resource == null) {
            throw new NoSuchFileException(address);
        }
        if (resource.getProtocol().equals("file")) {
            final URL root = this.anchor.getResource(this.resourceRoot);
            if (root == null || !root.getProtocol().equals("file")) {
                throw new NoSuchFileException(address);
            }
            try {
                return readDirectory(Path.of(root.toURI()), relative);
            } catch (final URISyntaxException error) {
                throw new IOException("Invalid classpath resource URI", error);
            }
        }
        if (resource.getProtocol().equals("jar")) {
            final JarURLConnection connection = (JarURLConnection) resource.openConnection();
            connection.setUseCaches(false);
            try (java.util.jar.JarFile jar = connection.getJarFile()) {
                final java.util.jar.JarEntry entry = jar.getJarEntry(name.substring(1));
                if (entry == null || entry.isDirectory()
                        || !name.substring(1).equals(connection.getEntryName())) {
                    throw new NoSuchFileException(address);
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    return input.readAllBytes();
                }
            }
        }
        throw new IOException("Unsupported classpath resource protocol");
    }

    /**
     * Reads a regular file after checking its real path against its own real root.
     */
    static byte[] readDirectory(final Path directory, final String relative) throws IOException {
        requireRelative(relative);
        final Path root = directory.toRealPath();
        final Path path = root.resolve(relative).toRealPath();
        if (!path.startsWith(root)) {
            throw new SecurityException("Static resource escaped its root");
        }
        if (!Files.isRegularFile(path)) {
            throw new NoSuchFileException(relative);
        }
        return Files.readAllBytes(path);
    }

    /**
     * Validates a decoded relative file path without decoding it again.
     */
    private static void requireRelative(final String path) {
        if (path.isEmpty() || path.startsWith("/") || path.endsWith("/")) {
            throw new SecurityException("Not a relative file path");
        }
        for (final String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new SecurityException("Invalid path segment");
            }
        }
        for (int index = 0; index < path.length(); index++) {
            final char ch = path.charAt(index);
            if (Character.isISOControl(ch) || "\\%?:#!".indexOf(ch) >= 0) {
                throw new SecurityException("Ambiguous static resource path");
            }
        }
    }

    /**
     * Normalizes configuration prefixes and rejects broad or ambiguous mounts.
     */
    private static String canonicalRoot(final String value) {
        Objects.requireNonNull(value, "prefix");
        if (!value.startsWith("/") || value.equals("/")) {
            throw new IllegalArgumentException("An absolute non-root prefix is required");
        }
        final String relative = value.substring(1,
            value.endsWith("/") ? value.length() - 1 : value.length());
        try {
            requireRelative(relative);
        } catch (final SecurityException error) {
            throw new IllegalArgumentException("Invalid static prefix", error);
        }
        return "/" + relative + "/";
    }
}
