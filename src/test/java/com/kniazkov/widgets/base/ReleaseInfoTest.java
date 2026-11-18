/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Real HTTP refuses stale clients before constructing UI or dispatching events.
 */
public class ReleaseInfoTest {
    /**
     * Missing and wrong build IDs fail even with valid session and update protocol versions.
     */
    @Test
    public void rejectsOldJavaScriptBeforeCreatingUi() throws Exception {
        final AtomicInteger creates = new AtomicInteger();
        try (Application app = new Application((root, context) -> {
            assertFalse(context.parameters.containsKey("clientBuild"));
            creates.incrementAndGet();
        })) {
            final int port = Server.start(app, new Options.Builder().setPort(0).build()).getPort();
            final ProtocolTestBrowser browser = new ProtocolTestBrowser(port);
            final Map<String, String> form = new TreeMap<>(Map.of("action", "new instance",
                "address", "/", "browserId", ProtocolTestBrowser.BROWSER_ID, "mobile", "false",
                "updateVersion", UpdateStream.VERSION,
                "updateCapability", UpdateStream.CAPABILITY));
            for (final String build : new String[]{"", "old-build", "0".repeat(64)}) {
                if (!build.isEmpty()) {
                    form.put("clientBuild", build);
                }
                final var reply = ProtocolTestBrowser.json(
                    browser.post(form, browser.headers(null)));
                assertTrue(reply.get("protocolMismatch").booleanValue());
                assertTrue(reply.get("clientBuildMismatch").booleanValue());
                assertEquals(ReleaseInfo.buildId(), reply.get("expectedBuild").stringValue());
                assertEquals(0, creates.get());
            }
            assertTrue(browser.create(null).get("id").isString());
            assertEquals(1, creates.get());
        }
    }

    /**
     * Bootstrap pins resources; stale and ambiguous URLs never serve the new JavaScript.
     */
    @Test
    public void pinsAssetsToLoadedArtifact() throws Exception {
        assertTrue(ReleaseInfo.buildId().matches("[0-9a-f]{64}"));
        assertTrue(ReleaseInfo.version().matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-SNAPSHOT)?"));
        try (Application app = new Application((root, context) -> { })) {
            final int port = Server.start(app, new Options.Builder().setPort(0).build()).getPort();
            final ProtocolTestBrowser browser = new ProtocolTestBrowser(port);
            assertEquals(ReleaseInfo.buildId(), browser.bootstrap.headers()
                .firstValue("X-Widgets-Build").orElseThrow());
            final String pinned = "?widgets-build=" + ReleaseInfo.buildId();
            final String html = browser.bootstrap.body();
            assertTrue(html.contains("/scripts/page-runtime.js" + pinned));
            assertTrue(html.contains("/style.css" + pinned));
            final HttpClient http = HttpClient.newHttpClient();
            final String path = "http://localhost:" + port + "/scripts/shared/transport.js";
            final var current = http.send(HttpRequest.newBuilder(URI.create(path + pinned)).build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, current.statusCode());
            assertTrue(current.body().contains("const widgetsClientBuild = \""
                + ReleaseInfo.buildId() + "\""));
            assertFalse(current.body().contains("__WIDGETS_BUILD__"));
            for (final String query : new String[]{"?widgets-build=old", pinned + pinned.replace(
                    '?', '&'), "?widgets-build="}) {
                final var stale = http.send(HttpRequest.newBuilder(
                    URI.create(path + query)).build(),
                    HttpResponse.BodyHandlers.ofString());
                assertEquals(404, stale.statusCode());
                assertEquals("no-store", stale.headers().firstValue("Cache-Control").orElseThrow());
                assertFalse(stale.body().contains("function sendHttpRequest"));
            }
        }
    }
}
