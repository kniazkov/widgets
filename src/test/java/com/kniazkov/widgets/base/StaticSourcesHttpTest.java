/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Exercises routing and hostile request targets through the real server.
 */
public class StaticSourcesHttpTest {
    /**
     * Filesystem fixture.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    /**
     * Listener stopped after each test.
     */
    private com.kniazkov.webserver.Server server;

    /**
     * Releases the listener even after assertion failures.
     */
    @After
    public void stop() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * Nested mounts win independently of registration order and never fall back.
     */
    @Test
    public void selectsLongestPrefixAndRetainsDefaultRoot() throws Exception {
        final Path www = this.folder.newFolder("www").toPath();
        final Path broad = this.folder.newFolder("broad").toPath();
        final Path narrow = this.folder.newFolder("narrow").toPath();
        Files.createDirectories(www.resolve("assets/deep"));
        Files.createDirectories(broad.resolve("deep"));
        Files.writeString(www.resolve("assets/deep/missing.txt"), "default-fallback");
        Files.writeString(broad.resolve("deep/missing.txt"), "broad-fallback");
        Files.writeString(broad.resolve("deep/ok.txt"), "wrong-source");
        Files.writeString(narrow.resolve("ok.txt"), "narrow-source");
        Files.writeString(broad.resolve("ok.txt"), "broad-source");
        Files.writeString(www.resolve("assets-other.txt"), "default-source");
        start(www, StaticSource.directory("/assets/deep", narrow),
            StaticSource.directory("/assets", broad));
        assertTrue(request("/assets/deep/ok.txt").endsWith("narrow-source"));
        assertTrue(request("/assets/ok.txt").endsWith("broad-source"));
        assertTrue(request("/assets-other.txt").endsWith("default-source"));
        assertTrue(request("/assets/deep/missing.txt").startsWith("HTTP/1.1 404"));
        assertTrue(request("/assets").startsWith("HTTP/1.1 404"));
        assertTrue(request("/assets/").startsWith("HTTP/1.1 404"));
    }

    /**
     * Registered pages and framework bootstrap still take precedence over static mounts.
     */
    @Test
    public void preservesPagesAndBundledResources() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        Files.createDirectories(www.resolve("assets"));
        Files.writeString(www.resolve("page"), "shadow-page");
        start(www, StaticSource.directory("/assets", www));
        final String page = request("/assets/page?value=42");
        assertTrue(page.startsWith("HTTP/1.1 200"));
        assertFalse(page.contains("shadow-page"));
        assertTrue(page.contains("configureUploadProtocol"));
        for (final String path : new String[]{"/", "/index.html", "/style.css",
                "/scripts/page-runtime.js"}) {
            assertTrue(path, request(path).startsWith("HTTP/1.1 200"));
        }
        assertTrue(request("/com/kniazkov/widgets/base/Application.class")
            .startsWith("HTTP/1.1 404"));
    }

    /**
     * Encoded names are decoded once; query strings are not part of the file name.
     */
    @Test
    public void servesEncodedNamesAndClasspathResources() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        Files.writeString(www.resolve("фото 1.txt"), "unicode-file");
        start(www, StaticSource.directory("/uploads", www),
            StaticSource.classpath("/icons", getClass(), "/static-test"));
        assertTrue(request("/uploads/%D1%84%D0%BE%D1%82%D0%BE%201.txt?v=2")
            .endsWith("unicode-file"));
        final String icon = request("/icons/icon.svg");
        assertTrue(icon.startsWith("HTTP/1.1 200"));
        assertTrue(icon.contains("image/svg+xml"));
        assertTrue(icon.contains("<svg"));
        assertTrue(request("/icons/com/kniazkov/widgets/base/Application.class")
            .startsWith("HTTP/1.1 404"));
    }

    /**
     * Neither parser normalization nor symlinks can expose a sibling private tree.
     */
    @Test
    public void blocksTraversalAndSymlinkEscapes() throws Exception {
        final Path www = this.folder.newFolder("www").toPath();
        final Path secret = this.folder.newFile("secret.txt").toPath();
        Files.writeString(secret, "private-marker");
        Files.createSymbolicLink(www.resolve("escape.txt"), secret);
        start(www, StaticSource.directory("/assets", www),
            StaticSource.classpath("/icons", getClass(), "/static-test"));
        for (final String prefix : new String[]{"/assets/", "/icons/", "/"}) {
            for (final String suffix : new String[]{"../secret.txt", "%2e%2e/secret.txt",
                    "%252e%252e/secret.txt", "..%2fsecret.txt", "..%5csecret.txt",
                    "%2fetc/passwd", "C%3asecret.txt", "icon.svg%00", "a//secret.txt"}) {
                final String path = prefix + suffix;
                final String response = request(path);
                assertFalse(path, response.startsWith("HTTP/1.1 200"));
                assertFalse(path, response.contains("private-marker"));
            }
        }
        assertTrue(request("/assets/escape.txt").startsWith("HTTP/1.1 403"));
        assertTrue(request("/escape.txt").startsWith("HTTP/1.1 403"));
    }

    /**
     * Starts an application with the supplied sources.
     */
    private void start(final Path www, final StaticSource... sources) {
        final Options.Builder options = new Options.Builder().setPort(0)
            .setBindAddress(InetAddress.getLoopbackAddress()).setWwwRoot(www.toString());
        for (final StaticSource source : sources) {
            options.addStaticSource(source);
        }
        final Page page = (widget, context) -> { };
        final Application application = BaseTestSupport.application(page);
        application.addPage("assets/page", page);
        this.server = Server.start(application, options.build());
    }

    /**
     * A later nested mount also wins, including when it rejects a file as forbidden.
     */
    @Test
    public void nestedMountCannotFallBackAfterForbiddenFile() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        final Path broad = this.folder.newFolder().toPath();
        final Path narrow = this.folder.newFolder().toPath();
        final Path secret = this.folder.newFile().toPath();
        Files.createDirectories(broad.resolve("deep"));
        Files.writeString(broad.resolve("deep/escape.txt"), "fallback");
        Files.writeString(secret, "private-marker");
        Files.createSymbolicLink(narrow.resolve("escape.txt"), secret);
        Files.writeString(narrow.resolve("script.js"), "log('keep'); {sessionId}");
        start(www, StaticSource.directory("/assets", broad),
            StaticSource.directory("/assets/deep", narrow));
        final String response = request("/assets/deep/escape.txt");
        assertTrue(response.startsWith("HTTP/1.1 403"));
        assertFalse(response.contains("fallback"));
        assertFalse(response.contains("private-marker"));
        assertTrue(request("/assets/deep/script.js").endsWith("log('keep'); {sessionId}"));
    }

    /**
     * Sends raw targets so a client cannot normalize away attempted traversal.
     */
    private String request(final String target) throws Exception {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), this.server.getPort())) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(("GET " + target + " HTTP/1.1\r\n"
                + "Host: localhost\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
