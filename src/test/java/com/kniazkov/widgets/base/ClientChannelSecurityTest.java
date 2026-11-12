/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.RootWidget;
import com.kniazkov.widgets.view.TextWidget;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end HTTP authorization, lifecycle, replay and revocation regressions.
 */
public final class ClientChannelSecurityTest {
    /**
     * Running listener.
     */
    private com.kniazkov.webserver.Server server;
    /**
     * Application under test.
     */
    private Application app;
    /**
     * Last created page.
     */
    private final AtomicReference<PageContext> context = new AtomicReference<>();
    /**
     * Last created root.
     */
    private final AtomicReference<RootWidget> root = new AtomicReference<>();
    /**
     * Last created control.
     */
    private final AtomicReference<Button> button = new AtomicReference<>();
    /**
     * Last created loader.
     */
    private final AtomicReference<FileLoader> loader = new AtomicReference<>();
    /**
     * Observable business mutations.
     */
    private final AtomicInteger clicks = new AtomicInteger();

    /**
     * Starts an isolated listener for each attack scenario.
     */
    @Before
    public void start() {
        this.app = new Application((widget, page) -> {
            this.context.set(page);
            this.root.set(widget);
            final Button action = new Button("Change private state");
            action.onClick(event -> this.clicks.incrementAndGet());
            this.button.set(action);
            final FileLoader upload = new FileLoader();
            this.loader.set(upload);
            widget.add(new Section(new TextWidget("private account data"), action, upload));
            widget.downloadFile("private.txt", "text/plain", new byte[]{11, 22, 33});
        });
        this.server = Server.start(this.app, new Options.Builder().setPort(0).build());
    }

    /**
     * Stops the listener.
     */
    @After
    public void stop() throws Exception {
        this.server.stop();
    }

    /**
     * Guessing a sequential ID or copying browser/tab metadata cannot read or mutate a page.
     */
    @Test
    public void rejectsForeignMissingAndIncorrectCredentialsOnEveryAction() throws Exception {
        final var victim = new ProtocolTestBrowser(this.server.getPort());
        final var attacker = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = victim.create(null);
        final String event = this.click("#1");
        assertNotEquals(victim.cookie, attacker.cookie);
        assertNotEquals(victim.csrf, attacker.csrf);
        assertEquals(43, channel.get("channelToken").stringValue().length());
        for (final String action : new String[]{
                "synchronize", "kill", "report error", "upload chunk"}) {
            for (int variant = 0; variant < 9; variant++) {
                final var headers = variant == 0
                    ? attacker.headers(channel) : victim.headers(channel);
                switch (variant) {
                    case 1 -> headers.remove("X-Widgets-Channel");
                    case 2 -> headers.put("X-Widgets-Channel", "x".repeat(43));
                    case 3 -> headers.remove("X-Widgets-CSRF");
                    case 4 -> headers.put("X-Widgets-CSRF", attacker.csrf);
                    case 5 -> headers.put("X-Widgets-Epoch", UUID.randomUUID().toString());
                    case 6 -> headers.remove("Cookie");
                    case 7 -> headers.remove("X-Widgets-Epoch");
                    case 8 -> headers.put("Cookie", victim.cookie + "; " + victim.cookie);
                    default -> { }
                }
                final HttpResponse<String> response = "upload chunk".equals(action)
                    ? this.upload(victim, channel, headers)
                    : victim.post(Map.of("action", action, "client", id(channel), "events", event,
                        "error", "forged diagnostic"), headers);
                assertEquals(action + ": " + variant, 403, response.statusCode());
                assertEquals("no-store",
                    response.headers().firstValue("Cache-Control").orElseThrow());
                assertFalse(response.body().contains("private"));
            }
        }
        assertEquals(0, this.clicks.get());
        final var own = this.sync(victim, channel, event);
        assertEquals(200, own.statusCode());
        assertTrue(own.body().contains("private account data"));
        assertTrue(own.body().contains("download file"));
        assertEquals(1, this.clicks.get());
    }

    /**
     * GET, foreign origins, fetch metadata and ambiguous headers never dispatch events.
     */
    @Test
    public void rejectsCsrfAndStateChangingGet() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        for (final String action : new String[]{"new%20instance", "synchronize", "kill",
                "report%20error", "upload%20chunk"}) {
            final var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create(browser.uri + "?action=" + action
                    + "&client=" + id(channel).replace("#", "%23")))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
            assertEquals("POST", response.headers().firstValue("Allow").orElseThrow());
        }
        for (final String origin : new String[]{
                "https://evil.example", "null", "http://localhost.evil",
                "http://user@localhost", "http://localhost/path"}) {
            final var headers = browser.headers(channel);
            headers.put("Origin", origin);
            assertEquals(403, browser.post(
                this.syncFields(channel, this.click("#1")), headers).statusCode());
        }
        final var headers = browser.headers(channel);
        headers.put("Sec-Fetch-Site", "same-site");
        assertEquals(403, browser.post(this.syncFields(channel, "[]"), headers).statusCode());
        headers.remove("Sec-Fetch-Site");
        headers.put("x-widgets-channel", channel.get("channelToken").stringValue());
        assertEquals(403, browser.post(this.syncFields(channel, "[]"), headers).statusCode());
        assertEquals(0, this.clicks.get());
        headers.remove("x-widgets-channel");
        headers.remove("Origin");
        assertEquals(200, browser.post(
            this.syncFields(channel, this.click("#1")), headers).statusCode());
        assertEquals(1, this.clicks.get());
    }

    /**
     * The owner may resume a document, but another browser cannot steal its context.
     */
    @Test
    public void bindsTabAndChannelsToVerifiedSession() throws Exception {
        final var first = new ProtocolTestBrowser(this.server.getPort());
        final var second = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject initial = first.create(null);
        final PageContext original = this.context.get();
        final String tab = initial.get("tabId").stringValue();
        final JsonObject next = first.create(tab);
        assertSame(original.browserSession, this.context.get().browserSession);
        assertSame(original.tabContext, this.context.get().tabContext);
        assertNotEquals(initial.get("channelToken"), next.get("channelToken"));
        assertTrue(second.create(tab).get("tabContextExpired").booleanValue());
        assertNotEquals(tab, second.create(null).get("tabId").stringValue());
        assertThrows(ProtocolAccessException.class,
            () -> this.app.killClient(RMId.parse(id(initial))));
    }

    /**
     * Idempotent event replay still works; a killed client cannot be resurrected.
     */
    @Test
    public void preservesOwnReplayAndAuthenticatedCleanup() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        final String event = this.click("#1");
        this.sync(browser, channel, event);
        this.sync(browser, channel, event);
        assertEquals(1, this.clicks.get());
        final var response = browser.post(Map.of("action", "kill", "client", id(channel)),
            browser.headers(channel));
        assertEquals(200, response.statusCode());
        assertFalse(ProtocolTestBrowser.json(this.sync(browser, channel, this.click("#2")))
            .get("clientAlive").booleanValue());
        assertEquals(1, this.clicks.get());
    }

    /**
     * Logout/user change invalidates every old channel; new bootstrap rotates both secrets.
     */
    @Test
    public void revokesWholeSessionAndRejectsOldSecretsAfterRotation() throws Exception {
        final var old = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject first = old.create(null);
        final BrowserSession session = this.context.get().browserSession;
        final JsonObject second = old.create(null);
        session.revoke();
        for (final JsonObject channel : new JsonObject[]{first, second}) {
            final var response = this.sync(old, channel, this.click("#1"));
            assertEquals(403, response.statusCode());
            assertTrue(ProtocolTestBrowser.json(response).get("sessionExpired").booleanValue());
        }
        assertEquals(403,
            old.post(Map.of("action", "new instance"), old.headers(null)).statusCode());
        final var fresh = new ProtocolTestBrowser(this.server.getPort());
        fresh.create(null);
        assertNotEquals(session.getId(), this.context.get().browserSession.getId());
        assertNotEquals(old.cookie, fresh.cookie);
        assertNotEquals(old.csrf, fresh.csrf);
        final var staleDocument = old.headers(first);
        staleDocument.put("Cookie", fresh.cookie);
        final var replacedCookie = fresh.post(this.syncFields(first, "[]"), staleDocument);
        assertEquals(403, replacedCookie.statusCode());
        assertTrue(ProtocolTestBrowser.json(replacedCookie).get("sessionExpired").booleanValue());
        assertEquals(403,
            fresh.post(this.syncFields(first, "[]"), fresh.headers(first)).statusCode());
        assertEquals(0, this.clicks.get());
    }

    /**
     * A revoke event suppresses subsequent events and private updates in the same request.
     */
    @Test
    public void stopsBatchAtRevocation() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        final BrowserSession session = this.context.get().browserSession;
        this.button.get().onClick(event -> {
            this.clicks.incrementAndGet();
            session.revoke();
        });
        final String batch = this.click("#1").replace("]", ",") + this.click("#2").substring(1);
        final var response = this.sync(browser, channel, batch);
        assertTrue(ProtocolTestBrowser.json(response).get("sessionExpired").booleanValue());
        assertFalse(response.body().contains("private"));
        assertFalse(response.body().contains("updates"));
        assertEquals(1, this.clicks.get());
    }

    /**
     * A request queued behind the root lock rechecks revocation before executing a click.
     */
    @Test
    public void rejectsRequestWaitingOnRootDuringLogout() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        final BrowserSession session = this.context.get().browserSession;
        final var worker = new AtomicReference<Thread>();
        final var original = BaseTestSupport.clients(this.app);
        BaseTestSupport.replaceClients(this.app, new java.util.concurrent.ConcurrentHashMap<>(
                original) {
            /**
             * Serialization version.
             */
            private static final long serialVersionUID = 1L;
            @Override
            public Client computeIfPresent(final RMId key,
                    final java.util.function.BiFunction<? super RMId, ? super Client,
                        ? extends Client> action) {
                worker.set(Thread.currentThread());
                return super.computeIfPresent(key, action);
            }
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            final var started = new CountDownLatch(1);
            final java.util.concurrent.Future<HttpResponse<String>> pending;
            synchronized (this.root.get()) {
                pending = executor.submit(() -> {
                    started.countDown();
                    return this.sync(browser, channel, this.click("#1"));
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean waiting = false;
                while (!waiting && System.nanoTime() < deadline) {
                    final Thread thread = worker.get();
                    waiting = thread != null && thread.getState() == Thread.State.BLOCKED
                        && java.util.Arrays.stream(thread.getStackTrace()).anyMatch(frame ->
                            frame.getClassName().equals(Client.class.getName())
                                && frame.getMethodName().equals("synchronize"));
                    if (!waiting) {
                        Thread.sleep(5);
                    }
                }
                assertTrue("HTTP request did not reach the held root lock", waiting);
                session.revoke();
            }
            final var response = pending.get(5, TimeUnit.SECONDS);
            assertFalse(response.body().contains("private"));
            assertEquals(0, this.clicks.get());
        }
    }

    /**
     * Authentication precedes diagnostic deduplication, and credentials never enter logs.
     */
    @Test
    public void rejectsForgedDiagnosticsAndRedactsCredentials() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final var attacker = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        final StringBuilder logs = new StringBuilder();
        final Handler capture = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                logs.append(record.getMessage());
            }
            @Override
            public void flush() {
            }
            @Override
            public void close() {
            }
        };
        final Logger logger = Logger.getLogger(Application.class.getName());
        logger.addHandler(capture);
        try {
            final Map<String, String> report = Map.of(
                "action", "report error", "client", id(channel),
                "error", "my error " + browser.csrf
                    + " " + channel.get("channelToken").stringValue());
            assertEquals(403, attacker.post(report, attacker.headers(channel)).statusCode());
            assertEquals("", logs.toString());
            assertEquals(200, browser.post(report, browser.headers(channel)).statusCode());
            assertTrue(logs.toString().contains("my error"));
            assertFalse(logs.toString().contains(browser.csrf));
            assertFalse(logs.toString().contains(channel.get("channelToken").stringValue()));
        } finally {
            logger.removeHandler(capture);
        }
    }

    /**
     * Bootstrap cookies are host-only, HttpOnly, Strict and never cacheable.
     */
    @Test
    public void issuesHardenedBootstrapAndValidatesProxyOrigin() throws Exception {
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final String cookie = browser.bootstrap.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/"));
        assertFalse(cookie.contains("Domain="));
        assertFalse(browser.bootstrap.body().contains(
            browser.cookie.substring(browser.cookie.indexOf('=') + 1)));
        assertEquals("no-store",
            browser.bootstrap.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("https://example.com", new Options.Builder()
            .setPublicOrigin("HTTPS://Example.com:443/")
            .build().getPublicOrigin());
        for (final String value : new String[]{
                "https://x/path", "https://user@x", "null", "https://x?q=1"}) {
            assertThrows(IllegalArgumentException.class,
                () -> new Options.Builder().setPublicOrigin(value));
        }
    }

    /**
     * An expired cookie cannot be renewed by an action or by replaying its channel secret.
     */
    @Test
    public void rejectsExpiredSessionOverHttp() throws Exception {
        this.server.stop();
        this.server = Server.start(this.app, new Options.Builder().setPort(0)
            .setClientLifetime(1000).build());
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final JsonObject channel = browser.create(null);
        final BrowserSession session = this.context.get().browserSession;
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (session.isActive() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(session.isActive());
        final var response = this.sync(browser, channel, this.click("#1"));
        assertEquals(403, response.statusCode());
        assertTrue(ProtocolTestBrowser.json(response).get("sessionExpired").booleanValue());
        assertFalse(session.touch());
        assertEquals(0, this.clicks.get());
    }

    /**
     * A configured TLS proxy origin controls both Secure cookies and Origin validation.
     */
    @Test
    public void honorsExplicitPublicOriginWithoutTrustingForwardedHeaders() throws Exception {
        this.server.stop();
        this.server = Server.start(this.app, new Options.Builder().setPort(0)
            .setPublicOrigin("https://shop.example").build());
        final var browser = new ProtocolTestBrowser(this.server.getPort());
        final String cookie = browser.bootstrap.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith("__Host-widgets-session="));
        assertTrue(cookie.contains("Secure"));
        final var headers = browser.headers(null);
        final Map<String, String> form = Map.of("action", "new instance", "address", "/",
            "browserId", ProtocolTestBrowser.BROWSER_ID, "mobile", "false");
        headers.put("Origin", "https://shop.example");
        assertEquals(200, browser.post(form, headers).statusCode());
        headers.put("Origin", "https://evil.example");
        headers.put("X-Forwarded-Host", "evil.example");
        headers.put("X-Forwarded-Proto", "https");
        assertEquals(403, browser.post(form, headers).statusCode());
    }

    /**
     * @param id monotonic event ID
     * @return click event batch
     */
    private String click(final String id) {
        return "[{\"id\":\"" + id + "\",\"widget\":\"" + this.button.get().getId()
            + "\",\"type\":\"click\",\"data\":" + EventTestData.pointer() + "}]";
    }

    /**
     * @param browser transport
     * @param channel created client
     * @param events event batch
     * @return sync response
     */
    private HttpResponse<String> sync(final ProtocolTestBrowser browser, final JsonObject channel,
            final String events) throws Exception {
        return browser.post(this.syncFields(channel, events), browser.headers(channel));
    }

    /**
     * @param channel created client
     * @param events batch
     * @return sync form fields
     */
    private Map<String, String> syncFields(final JsonObject channel, final String events) {
        return Map.of("action", "synchronize", "client", id(channel), "events", events);
    }

    /**
     * @param browser transport
     * @param channel target
     * @param headers request authentication
     * @return upload response
     */
    private HttpResponse<String> upload(final ProtocolTestBrowser browser, final JsonObject channel,
            final Map<String, String> headers) throws Exception {
        final String boundary = "widgets-security-boundary";
        final StringBuilder body = new StringBuilder();
        Map.of("action", "upload chunk", "client", id(channel),
            "widget", this.loader.get().getId().toString(),
            "fileId", "1", "chunkIndex", "0").forEach((key, value) -> body
                .append("--").append(boundary)
                .append("\r\nContent-Disposition: form-data; name=\"").append(key)
                .append("\"\r\n\r\n").append(value).append("\r\n"));
        body.append("--").append(boundary)
            .append("\r\nContent-Disposition: form-data; name=\"chunk\"; filename=\"test.txt\"")
            .append("\r\nContent-Type: application/octet-stream\r\n\r\nsecret\r\n--")
            .append(boundary).append("--\r\n");
        return browser.send("multipart/form-data; boundary=" + boundary, body.toString(), headers);
    }

    /**
     * @param channel creation response
     * @return public client ID
     */
    private static String id(final JsonObject channel) {
        return channel.get("id").stringValue();
    }
}
