/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonException;
import com.kniazkov.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads browser source groups from the manifest shared with the JavaScript test harness.
 * Widget and client scripts run inside one lexical scope per retained page, not globally.
 */
final class BrowserScripts {
    /**
     * Prevents construction of this resource utility.
     */
    private BrowserScripts() {
    }

    /**
     * Reads a UTF-8 script resource relative to the scripts directory.
     *
     * @param path bundled resource path
     * @return resource text
     * @throws IOException if a required resource is missing or unreadable
     */
    static String read(final String path) throws IOException {
        try (InputStream input = BrowserScripts.class.getResourceAsStream("/scripts/" + path)) {
            if (input == null) {
                throw new IOException("Missing browser resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns a group's source paths in loading order.
     *
     * @param group manifest group name
     * @return immutable source paths
     * @throws IOException if the manifest or group is invalid
     */
    static List<String> paths(final String group) throws IOException {
        try {
            final JsonObject manifest = Json.parse(read("manifest.json")).toJsonObject();
            final JsonArray entries = manifest == null ? null : manifest.get(group).toJsonArray();
            if (entries == null || entries.isEmpty()) {
                throw new IOException("Missing browser script group: " + group);
            }
            final List<String> paths = new ArrayList<>();
            for (final JsonElement entry : entries) {
                final String path = entry.getStringValue();
                if (path == null || !path.matches("[a-z-]+/[a-z-]+\\.js")) {
                    throw new IOException("Invalid browser script path in group: " + group);
                }
                paths.add(path);
            }
            return List.copyOf(paths);
        } catch (final JsonException failure) {
            throw new IOException("Invalid browser script manifest", failure);
        }
    }

    /**
     * Creates ordered script tags for stateless helpers shared across pages.
     *
     * @return HTML script elements
     * @throws IOException if the manifest cannot be read
     */
    static String sharedTags() throws IOException {
        final StringBuilder tags = new StringBuilder();
        for (final String path : paths("shared")) {
            tags.append("<script src=\"/scripts/").append(path).append("\"></script>\n");
        }
        return tags.toString();
    }

    /**
     * Builds the classic-script factory without exposing per-page state to the window.
     *
     * @return page runtime factory source
     * @throws IOException if a required source cannot be read
     */
    static String pageRuntime() throws IOException {
        final StringBuilder source = new StringBuilder("function createPageRuntime(page) {\n");
        for (final String group : List.of("widgets", "client")) {
            for (final String path : paths(group)) {
                source.append("\n// Source: /scripts/").append(path).append('\n');
                source.append(read(path)).append('\n');
            }
        }
        source.append("return { initClient, mainCycle, disposeClient, showClientError };\n}\n");
        return source.toString();
    }
}
