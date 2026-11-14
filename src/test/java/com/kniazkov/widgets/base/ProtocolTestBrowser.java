/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonParsingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Independent real HTTP browser credentials; no access to server internals.
 */
final class ProtocolTestBrowser {
    /**
     * Transport.
     */
    private final HttpClient http = HttpClient.newHttpClient();
    /**
     * Application endpoint.
     */
    final URI uri;
    /**
     * Bootstrap response.
     */
    final HttpResponse<String> bootstrap;
    /**
     * Browser cookie header.
     */
    final String cookie;
    /**
     * Bootstrap token.
     */
    final String csrf;
    /**
     * Public document generation.
     */
    final String session;
    /**
     * Deliberately shared, untrusted browser metadata used by attack tests.
     */
    static final String BROWSER_ID = "00000000-0000-0000-0000-000000000001";

    /**
     * @param port actual listening port
     */
    ProtocolTestBrowser(final int port) throws Exception {
        this.uri = URI.create("http://localhost:" + port + "/");
        this.bootstrap = this.http.send(HttpRequest.newBuilder(this.uri).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        this.cookie = this.bootstrap.headers().firstValue("Set-Cookie")
            .orElseThrow().split(";", 2)[0];
        this.csrf = csrf(this.bootstrap.body());
        final var sessionMatch = Pattern.compile("configureProtocolSecurity.*?, \"([^\"]+)\"")
            .matcher(this.bootstrap.body());
        if (!sessionMatch.find()) {
            throw new IllegalStateException("Missing session generation");
        }
        this.session = sessionMatch.group(1);
    }

    /**
     * @param html bootstrap source
     * @return document token
     */
    static String csrf(final String html) {
        final var match = Pattern.compile("configureProtocolSecurity\\(\"([^\"]+)\"")
            .matcher(html);
        if (!match.find()) {
            throw new IllegalStateException("Missing protocol bootstrap");
        }
        return match.group(1);
    }

    /**
     * @param tab optional existing document
     * @return created client
     */
    JsonObject create(final String tab) throws Exception {
        final Map<String, String> fields = new TreeMap<>(Map.of("action", "new instance",
            "address", "/", "browserId", BROWSER_ID, "mobile", "false",
            "updateVersion", UpdateStream.VERSION, "updateCapability", UpdateStream.CAPABILITY));
        if (tab != null) {
            fields.put("tabId", tab);
        }
        return json(this.post(fields, this.headers(null)));
    }

    /**
     * @param channel optional channel response
     * @return independently editable authentication headers
     */
    Map<String, String> headers(final JsonObject channel) {
        final Map<String, String> headers = new TreeMap<>(Map.of("Cookie", this.cookie,
            "X-Widgets-CSRF", this.csrf, "X-Widgets-Session", this.session,
            "Origin", this.uri.toString().replaceAll("/$", "")));
        if (channel != null) {
            headers.put("X-Widgets-Channel", channel.get("channelToken").stringValue());
            headers.put("X-Widgets-Epoch", channel.get("serverId").stringValue());
            headers.put("X-Widgets-Update-Epoch", channel.get("updateEpoch").stringValue());
        }
        return headers;
    }

    /**
     * @param fields form fields
     * @param headers request headers
     * @return HTTP response
     */
    HttpResponse<String> post(final Map<String, String> fields, final Map<String, String> headers)
            throws Exception {
        final String form = fields.entrySet().stream().map(entry -> encode(entry.getKey()) + "="
            + encode(entry.getValue())).collect(Collectors.joining("&"));
        return this.send("application/x-www-form-urlencoded", form, headers);
    }

    /**
     * @param type content type
     * @param body serialized request
     * @param headers authentication headers
     * @return HTTP response
     */
    HttpResponse<String> send(final String type, final String body,
            final Map<String, String> headers) throws Exception {
        final var builder = HttpRequest.newBuilder(this.uri)
            .header("Content-Type", type).POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * @param response protocol response
     * @return parsed JSON
     */
    static JsonObject json(final HttpResponse<String> response)
            throws com.kniazkov.json.JsonException {
        return Json.parse(response.body(), JsonParsingMode.STRICT).objectValueOrThrow();
    }

    /**
     * @param text field text
     * @return encoded form value
     */
    private static String encode(final String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
