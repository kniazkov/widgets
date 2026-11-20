/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;

/**
 * Identity of the loaded Java classes and browser resources, computed once per class loader.
 * Supports ordinary class directories, library JARs and assembled application JARs.
 * This is compatibility metadata, not authentication or a cryptographic artifact signature.
 */
public final class ReleaseInfo {
    /**
     * Filtered Maven release descriptor.
     */
    private static final Properties DESCRIPTOR = descriptor();

    /**
     * Stable identity across a class directory and its packaged JAR.
     */
    private static final String BUILD = fingerprint();

    /**
     * Static utility.
     */
    private ReleaseInfo() {
    }

    /**
     * @return Maven version embedded in this artifact
     */
    public static String version() {
        return DESCRIPTOR.getProperty("version");
    }

    /**
     * @return HTTP response envelope version
     */
    public static String httpProtocol() {
        return DESCRIPTOR.getProperty("httpProtocol");
    }

    /**
     * @return ordered update envelope version
     */
    public static String updateProtocol() {
        return DESCRIPTOR.getProperty("updateProtocol");
    }

    /**
     * @return SHA-256 of loaded library classes and browser resources
     */
    public static String buildId() {
        return BUILD;
    }

    /**
     * @return explicit rejection understood by clients predating bundle negotiation
     */
    static JsonObject mismatch() {
        final JsonObject result = new JsonObject();
        result.addBoolean("protocolMismatch", true);
        result.addBoolean("clientBuildMismatch", true);
        result.addString("expectedBuild", BUILD);
        result.addString("releaseVersion", version());
        return result;
    }

    /**
     * @return validated version descriptor
     */
    private static Properties descriptor() {
        final Properties result = new Properties();
        try (InputStream stream = ReleaseInfo.class.getResourceAsStream(
                "/widgets-release.properties")) {
            if (stream == null) {
                throw new IOException("Missing release descriptor; build with Maven");
            }
            result.load(stream);
            if (!result.getProperty("version", "").matches(
                    "[0-9]+\\.[0-9]+\\.[0-9]+(?:-SNAPSHOT)?")) {
                throw new IOException("Invalid release version");
            }
            for (final String key : List.of("httpProtocol", "updateProtocol", "bundleProtocol")) {
                if (!result.getProperty(key, "").matches("[1-9][0-9]*")) {
                    throw new IOException("Invalid protocol version: " + key);
                }
            }
            return result;
        } catch (final IOException failure) {
            throw new IllegalStateException("Cannot read Widgets release metadata", failure);
        }
    }

    /**
     * @param name entry relative to the classpath root
     * @return whether it belongs to the Widgets runtime contract
     */
    private static boolean included(final String name) {
        return name.startsWith("com/kniazkov/widgets/") && name.endsWith(".class")
            || name.startsWith("scripts/") || name.startsWith("fonts/")
            || List.of("index.html", "style.css", "widgets-release.properties").contains(name);
    }

    /**
     * @param location directory or JAR containing one or more Widgets components
     * @param entries names to include in the fingerprint
     * @throws IOException when classes or resources cannot be enumerated
     */
    private static void collect(final Path location, final Set<String> entries) throws IOException {
        if (Files.isDirectory(location)) {
            try (var files = Files.walk(location)) {
                files.filter(Files::isRegularFile)
                    .map(file -> location.relativize(file).toString().replace('\\', '/'))
                    .filter(ReleaseInfo::included).forEach(entries::add);
            }
        } else {
            try (JarFile jar = new JarFile(location.toFile())) {
                jar.stream().filter(entry -> !entry.isDirectory()).map(entry -> entry.getName())
                    .filter(ReleaseInfo::included).forEach(entries::add);
            }
        }
    }

    /**
     * @return digest independent of ZIP timestamps, entry order and unrelated consumer classes
     */
    private static String fingerprint() {
        try {
            final Set<Path> locations = new TreeSet<>();
            locations.add(Path.of(ReleaseInfo.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()));
            final var markers = ReleaseInfo.class.getClassLoader()
                .getResources("META-INF/widgets-component");
            while (markers.hasMoreElements()) {
                final var marker = markers.nextElement();
                if (marker.openConnection() instanceof JarURLConnection jar) {
                    locations.add(Path.of(jar.getJarFileURL().toURI()));
                } else {
                    locations.add(Path.of(marker.toURI()).getParent().getParent());
                }
            }
            final Set<String> entries = new TreeSet<>();
            for (final Path location : locations) {
                collect(location, entries);
            }
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (final String entry : entries) {
                digest.update(entry.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                try (InputStream stream = ReleaseInfo.class.getResourceAsStream("/" + entry)) {
                    if (stream == null) {
                        throw new IOException("Missing bundle entry: " + entry);
                    }
                    final byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        digest.update(buffer, 0, count);
                    }
                }
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (final IOException | URISyntaxException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot identify Widgets bundle", failure);
        }
    }
}
