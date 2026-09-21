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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Exercises routing and hostile request targets through the real server.
 */
public class StaticSourcesHttpTest {
    /**
     * Versioned URLs opt into long caching only for bytes matching their requested hash.
     */
    @Test
    public void cachesOnlyMatchingContentVersions() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        final Path file = www.resolve("фото 1.svg");
        Files.writeString(file, "old-image");
        final StaticSource source = StaticSource.directory("/uploads", www);
        final StaticSource icons = StaticSource.classpath("/icons", getClass(), "/static-test");
        start(www, source, icons);
        final String url = source.versionedUrl("фото 1.svg");
        assertEquals(url, source.versionedUrl("фото 1.svg"));
        for (final String target : new String[]{url, icons.versionedUrl("icon.svg")}) {
            final String first = request(target);
            assertTrue(first, first.startsWith("HTTP/1.1 200"));
            assertEquals("private, max-age=31536000, immutable", header(first, "Cache-Control"));
            final String cached = request(target,
                "If-None-Match: " + header(first, "ETag") + "\r\n");
            assertTrue(cached, cached.startsWith("HTTP/1.1 304"));
            assertEquals(header(first, "Cache-Control"), header(cached, "Cache-Control"));
        }
        Files.writeString(file, "new-image");
        final String stale = request(url, "If-None-Match: *\r\n");
        assertTrue(stale, stale.startsWith("HTTP/1.1 404"));
        assertEquals("no-store", header(stale, "Cache-Control"));
        assertFalse(stale.contains("new-image"));
        final String updated = source.versionedUrl("фото 1.svg");
        assertNotEquals(url, updated);
        assertTrue(request(updated).endsWith("new-image"));
        for (final String bad : new String[]{updated + "&widgets-version=bad",
                "/icons/icon.svg?widgets-version=", "/icons/icon.svg?widgets-version=fake"}) {
            assertTrue(request(bad).startsWith("HTTP/1.1 404"));
        }
        assertEquals("private, no-cache", header(request("/icons/icon.svg?v=anything"),
            "Cache-Control"));
        Files.delete(file);
        assertTrue(request(updated).startsWith("HTTP/1.1 404"));
    }

    /**
     * A cached version cannot be reused after the file becomes an external symlink.
     */
    @Test
    public void rejectsVersionedFileReplacedByExternalSymlink() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path www = this.folder.newFolder().toPath();
        final Path file = www.resolve("logo.svg");
        Files.writeString(file, "image");
        final StaticSource source = StaticSource.directory("/uploads", www);
        final String url = source.versionedUrl("logo.svg");
        start(www, source);
        assertTrue(request(url).startsWith("HTTP/1.1 200"));
        final Path secret = this.folder.newFile().toPath();
        Files.writeString(secret, "image");
        Files.delete(file);
        Files.createSymbolicLink(file, secret);
        assertTrue(request(url, "If-None-Match: *\r\n").startsWith("HTTP/1.1 403"));
    }

    /**
     * File roots, classpath assets and generated scripts all support browser revalidation.
     */
    @Test
    public void revalidatesStaticBytesWithoutResponseBody() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        Files.writeString(www.resolve("photo.png"), "image-bytes");
        start(www, StaticSource.directory("/uploads", www),
            StaticSource.classpath("/icons", getClass(), "/static-test"));
        for (final String path : new String[]{"/photo.png", "/uploads/photo.png",
                "/icons/icon.svg", "/style.css", "/scripts/page-runtime.js"}) {
            final String first = request(path);
            final String tag = header(first, "ETag");
            assertTrue(first, tag.matches("\"[0-9a-f]{64}\""));
            assertEquals("private, no-cache", header(first, "Cache-Control"));
            for (final String condition : new String[]{tag, "W/" + tag, "*",
                    "\"other,tag\", W/" + tag, "\"other\"\r\nIf-None-Match: " + tag}) {
                final String cached = request(path, "iF-nOnE-mAtCh: " + condition + "\r\n");
                assertTrue(cached, cached.startsWith("HTTP/1.1 304"));
                assertEquals("", cached.substring(cached.indexOf("\r\n\r\n") + 4));
                assertEquals(tag, header(cached, "ETag"));
                assertEquals("private, no-cache", header(cached, "Cache-Control"));
                assertFalse(header(cached, "Date").isEmpty());
            }
            for (final String condition : new String[]{"\"other\"", "garbage " + tag,
                    tag + "garbage", "\"other\"" + tag, "*, " + tag}) {
                assertTrue(request(path, "If-None-Match: " + condition + "\r\n")
                    .startsWith("HTTP/1.1 200"));
            }
        }
    }

    /**
     * Replacing or deleting a file cannot reuse stale bytes.
     */
    @Test
    public void validatesCurrentFileBeforeConditionalResponse() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        final Path logo = www.resolve("logo.svg");
        Files.writeString(logo, "old-image");
        start(www);
        final String tag = header(request("/logo.svg"), "ETag");
        final var time = Files.getLastModifiedTime(logo);
        Files.writeString(logo, "new-image");
        Files.setLastModifiedTime(logo, time);
        final String replaced = request("/logo.svg", "If-None-Match: " + tag + "\r\n");
        assertTrue(replaced, replaced.startsWith("HTTP/1.1 200"));
        assertTrue(replaced.endsWith("new-image"));
        assertNotEquals(tag, header(replaced, "ETag"));
        Files.delete(logo);
        assertTrue(request("/logo.svg", "If-None-Match: *\r\n").startsWith("HTTP/1.1 404"));
    }

    /**
     * Conditional responses cannot reuse bytes from a file replaced by an external symlink.
     */
    @Test
    public void conditionalResponseRejectsExternalSymlink() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path www = this.folder.newFolder().toPath();
        final Path logo = www.resolve("logo.svg");
        Files.writeString(logo, "image");
        start(www);
        final String tag = header(request("/logo.svg"), "ETag");
        final Path secret = this.folder.newFile().toPath();
        Files.writeString(secret, "image");
        Files.delete(logo);
        Files.createSymbolicLink(logo, secret);
        for (final String condition : new String[]{tag, "*"}) {
            assertTrue(request("/logo.svg", "If-None-Match: " + condition + "\r\n")
                .startsWith("HTTP/1.1 403"));
        }
    }

    /**
     * Conditional headers must not suppress page bootstrap or action execution.
     */
    @Test
    public void doesNotRevalidateDynamicPagesOrActions() throws Exception {
        start(this.folder.newFolder().toPath());
        final String page = request("/assets/page?value=42", "If-None-Match: *\r\n");
        assertTrue(page, page.startsWith("HTTP/1.1 200"));
        assertEquals("no-store", header(page, "Cache-Control"));
        assertEquals("", header(page, "ETag"));
        assertTrue(page.contains("configureUploadProtocol"));
        assertTrue(request("/?action=unknown", "If-None-Match: *\r\n")
            .startsWith("HTTP/1.1 404"));
    }

    /**
     * Returns a header independent of the transport's header-name capitalization.
     */
    private static String header(final String response, final String name) {
        final String headers = response.substring(0, response.indexOf("\r\n\r\n"));
        for (final String line : headers.split("\r\n")) {
            final int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).equalsIgnoreCase(name)) {
                return line.substring(colon + 1).trim();
            }
        }
        return "";
    }

    /**
     * Uses the webserver MIME catalog, including its safe unknown-extension fallback.
     */
    @Test
    public void servesWebserverContentTypes() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        Files.createDirectories(www.resolve("folder.svg"));
        final String[][] cases = {
            {"photo.JPG", "image/jpeg"}, {"icon.SVG", "image/svg+xml"},
            {"photo.webp", "image/webp"}, {"font.woff2", "font/woff2"},
            {"unknown.xyzzy", "application/octet-stream"},
            {"svg", "application/octet-stream"},
            {"folder.svg/plain", "application/octet-stream"}
        };
        for (final String[] item : cases) {
            Files.writeString(www.resolve(item[0]), "content");
        }
        start(www, StaticSource.directory("/assets", www));
        for (final String[] item : cases) {
            for (final String prefix : new String[]{"/", "/assets/"}) {
                final String response = request(prefix + item[0]);
                assertTrue(response, response.startsWith("HTTP/1.1 200"));
                assertTrue(response, response.contains("Content-Type: " + item[1]));
            }
        }
    }

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
     * Parser normalization cannot expose a sibling private tree.
     */
    @Test
    public void blocksTraversalEscapes() throws Exception {
        final Path www = this.folder.newFolder("www").toPath();
        final Path secret = this.folder.newFile("secret.txt").toPath();
        Files.writeString(secret, "private-marker");
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
    }

    /**
     * Both the default root and an explicit mount reject external symlinks.
     */
    @Test
    public void blocksSymlinkEscapes() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path www = this.folder.newFolder().toPath();
        final Path secret = this.folder.newFile().toPath();
        Files.writeString(secret, "private-marker");
        Files.createSymbolicLink(www.resolve("escape.txt"), secret);
        start(www, StaticSource.directory("/assets", www));
        for (final String path : new String[]{"/assets/escape.txt", "/escape.txt"}) {
            final String response = request(path);
            assertTrue(response.startsWith("HTTP/1.1 403"));
            assertFalse(response.contains("private-marker"));
        }
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
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path www = this.folder.newFolder().toPath();
        final Path broad = this.folder.newFolder().toPath();
        final Path narrow = this.folder.newFolder().toPath();
        final Path secret = this.folder.newFile().toPath();
        Files.createDirectories(broad.resolve("deep"));
        Files.writeString(broad.resolve("deep/escape.txt"), "fallback");
        Files.writeString(secret, "private-marker");
        Files.createSymbolicLink(narrow.resolve("escape.txt"), secret);
        start(www, StaticSource.directory("/assets", broad),
            StaticSource.directory("/assets/deep", narrow));
        final String response = request("/assets/deep/escape.txt");
        assertTrue(response.startsWith("HTTP/1.1 403"));
        assertFalse(response.contains("fallback"));
        assertFalse(response.contains("private-marker"));
    }

    /**
     * Nested mounts serve scripts literally without application template substitution.
     */
    @Test
    public void nestedMountServesScriptsLiterally() throws Exception {
        final Path www = this.folder.newFolder().toPath();
        final Path broad = this.folder.newFolder().toPath();
        final Path narrow = this.folder.newFolder().toPath();
        Files.createDirectories(broad.resolve("deep"));
        Files.writeString(broad.resolve("deep/script.js"), "fallback");
        Files.writeString(narrow.resolve("script.js"), "log('keep'); {sessionId}");
        start(www, StaticSource.directory("/assets", broad),
            StaticSource.directory("/assets/deep", narrow));
        assertTrue(request("/assets/deep/script.js").endsWith("log('keep'); {sessionId}"));
    }

    /**
     * Sends raw targets so a client cannot normalize away attempted traversal.
     */
    private String request(final String target) throws Exception {
        return request(target, "");
    }

    /**
     * Sends optional conditional headers over a real HTTP connection.
     */
    private String request(final String target, final String headers) throws Exception {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), this.server.getPort())) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(("GET " + target + " HTTP/1.1\r\n"
                + "Host: localhost\r\n" + headers + "Connection: close\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
