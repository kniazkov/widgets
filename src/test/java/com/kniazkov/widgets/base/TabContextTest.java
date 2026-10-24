/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.view.Button;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Browser-bound context identity, terminal revocation and legacy protocol contracts.
 */
public final class TabContextTest {
    /**
     * Two tabs share a browser but not their context, while navigation shares one object.
     */
    @Test
    public void separatesTabsAndResolvesNavigation() {
        final AtomicReference<PageContext> captured = new AtomicReference<>();
        final Application app = BaseTestSupport.application(
            (root, context) -> captured.set(context));
        final UUID browser = UUID.randomUUID();
        final JsonObject first = create(app, browser, null);
        final TabContext tab = captured.get().tabContext;
        final JsonObject next = create(app, browser, first.get("tabId").stringValue());
        assertSame(tab, captured.get().tabContext);
        assertEquals(browser, captured.get().browserId);
        assertFalse(captured.get().parameters.containsKey("tabId"));
        assertNotEquals(first.get("id"), next.get("id"));
        final JsonObject independent = create(app, browser, null);
        assertNotEquals(first.get("tabId"), independent.get("tabId"));
        assertEquals(browser, captured.get().tabContext.getBrowserId());
    }

    /**
     * Bad, foreign, revoked and previous-server IDs cannot start a page.
     */
    @Test
    public void rejectsInvalidTokensAndOldServerState() {
        final AtomicInteger creations = new AtomicInteger();
        final AtomicReference<TabContext> captured = new AtomicReference<>();
        final Application app = BaseTestSupport.application((root, context) -> {
            creations.incrementAndGet();
            captured.set(context.tabContext);
        });
        final UUID browser = UUID.randomUUID();
        final String token = create(app, browser, null).get("tabId").stringValue();
        for (final String bad : new String[]{"", "invalid", UUID.randomUUID().toString()}) {
            assertTrue(create(app, browser, bad).get("tabContextExpired").booleanValue());
        }
        assertTrue(create(app, UUID.randomUUID(), token).get("tabContextExpired").booleanValue());
        final Application restarted = BaseTestSupport.application((root, context) -> { });
        assertTrue(create(restarted, browser, token).get("tabContextExpired").booleanValue());
        captured.get().revoke();
        assertTrue(create(app, browser, token).get("tabContextExpired").booleanValue());
        assertEquals(1, creations.get());
        assertNotEquals(token, create(app, browser, null).get("tabId").stringValue());
    }

    /**
     * Old client event batches cannot revive a revoked context or perform further actions.
     */
    @Test
    public void rejectsQueuedEventsAndUploadsAfterRevocation() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Button> action = new AtomicReference<>();
        final AtomicReference<TabContext> captured = new AtomicReference<>();
        final Application app = BaseTestSupport.application((root, context) -> {
            captured.set(context.tabContext);
            final Button button = new Button("End context");
            button.onClick(event -> {
                calls.incrementAndGet();
                context.tabContext.revoke();
            });
            root.add(new com.kniazkov.widgets.view.Section(button));
            action.set(button);
        });
        final JsonObject created = create(app, UUID.randomUUID(), null);
        final String client = created.get("id").stringValue();
        final String widget = action.get().getId().toString();
        final String events = "[{\"id\":\"#1\",\"widget\":\"" + widget
            + "\",\"type\":\"click\"},{\"id\":\"#2\",\"widget\":\"" + widget
            + "\",\"type\":\"click\"}]";
        final JsonObject response = new Synchronize(app).process(Map.of(
            "client", client, "events", events)).objectValueOrThrow();
        assertEquals(1, calls.get());
        assertTrue(response.get("tabContextExpired").booleanValue());
        assertFalse(captured.get().isActive());
        new Synchronize(app).process(Map.of("client", client, "events", events));
        assertEquals(1, calls.get());
        assertSame(com.kniazkov.widgets.common.UploadProtocol.rejected(), app.uploadChunk(
            RMId.parse(client), action.get().getId(), 1, 0, new byte[0], "#0"));
    }

    /**
     * Activity renews only live contexts; expired or explicitly revoked objects stay dead.
     */
    @Test
    public void expirationCannotBeReversed() {
        final AtomicLong clock = new AtomicLong();
        final TabContext tab = new TabContext(UUID.randomUUID(), 10, clock::get);
        clock.set(9_000_000);
        assertTrue(tab.touch());
        clock.set(18_000_000);
        assertTrue(tab.isActive());
        clock.set(19_000_000);
        assertFalse(tab.touch());
        clock.set(0);
        assertFalse(tab.isActive());
        assertFalse(tab.touch());
    }

    /**
     * @param app application
     * @param browser browser identity
     * @param token optional context
     * @return protocol response
     */
    private static JsonObject create(
        final Application app, final UUID browser, final String token
    ) {
        final Map<String, String> request = new TreeMap<>(Map.of(
            "action", "new instance", "address", "/", "browserId", browser.toString(),
            "mobile", "false"));
        if (token != null) {
            request.put("tabId", token);
        }
        return new CreateClient(app).process(request).objectValueOrThrow();
    }
}
