/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.InlineBlock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Every event must resolve its target against the tree left by preceding callbacks.
 */
public final class LiveEventTargetsTest {
    /**
     * Removal, ancestor removal, replacement and moving out never invoke stale controllers.
     */
    @Test
    public void skipsTargetsNoLongerInTheCurrentTree() {
        for (int mode = 0; mode < 4; mode++) {
            final int operation = mode;
            final Client client = new Client();
            final Client foreign = new Client();
            final Button first = new Button("Change tree");
            final Button stale = new Button("Old target");
            final Button following = new Button("Following action");
            final Section subtree = new Section(stale);
            final InlineBlock holder = new InlineBlock(subtree);
            final AtomicInteger calls = new AtomicInteger();
            stale.onClick(event -> calls.addAndGet(100));
            following.onClick(event -> calls.incrementAndGet());
            client.getRootWidget().add(new Section(first, holder, following));
            first.onClick(event -> {
                switch (operation) {
                    case 0 -> stale.remove();
                    case 1 -> subtree.remove();
                    case 2 -> {
                        holder.removeAll();
                        holder.add(new Section(new Button("Replacement")));
                    }
                    case 3 -> {
                        subtree.remove();
                        foreign.getRootWidget().add(subtree);
                    }
                    default -> throw new AssertionError(operation);
                }
            });
            final JsonObject response = send(client, first, stale, following);
            assertStatuses(response, "applied", "obsolete", "applied");
            assertEquals("detachedWidget", response.get("eventResults").arrayValueOrThrow()
                .get(1).objectValueOrThrow().get("reason").stringValue());
            assertEquals("#3", response.get("lastEvent").stringValue());
            assertEquals(1, calls.get());
        }
    }

    /**
     * An attached replacement is visible to the very next event in the same batch.
     */
    @Test
    public void resolvesNewlyAttachedReplacement() {
        final Client client = new Client();
        final Button replace = new Button("Replace");
        final Button old = new Button("Old");
        final Button replacement = new Button("New");
        final Section holder = new Section(old);
        final AtomicInteger calls = new AtomicInteger();
        old.onClick(event -> calls.addAndGet(100));
        replacement.onClick(event -> calls.incrementAndGet());
        replace.onClick(event -> {
            holder.removeAll();
            holder.add(replacement);
        });
        client.getRootWidget().add(new Section(replace));
        client.getRootWidget().add(holder);
        final JsonObject response = send(client, replace, old, replacement);
        assertStatuses(response, "applied", "obsolete", "applied");
        assertEquals(1, calls.get());
        assertEquals("#3", response.get("lastEvent").stringValue());
    }

    /**
     * Moving within the same root keeps identity but uses the new ancestor's state.
     */
    @Test
    public void rechecksAncestryAfterMovingWithinRoot() {
        for (final boolean hidden : new boolean[]{false, true}) {
            final Client client = new Client();
            final Button move = new Button("Move");
            final Button target = new Button("Target");
            final Section destination = new Section();
            if (hidden) {
                destination.hide();
            }
            final AtomicInteger calls = new AtomicInteger();
            target.onClick(event -> calls.incrementAndGet());
            move.onClick(event -> {
                target.remove();
                destination.add(target);
            });
            client.getRootWidget().add(new Section(move, target));
            client.getRootWidget().add(destination);
            assertStatuses(send(client, move, target), "applied", hidden ? "rejected" : "applied");
            assertEquals(hidden ? 0 : 1, calls.get());
        }
    }

    /**
     * A target imported from another root can receive events only after attachment here.
     */
    @Test
    public void resolvesTargetMovedIntoRoot() {
        final Client client = new Client();
        final Client foreign = new Client();
        final Button attach = new Button("Attach");
        final Button target = new Button("Imported");
        final AtomicInteger calls = new AtomicInteger();
        target.onClick(event -> calls.incrementAndGet());
        foreign.getRootWidget().add(new Section(target));
        final Section destination = new Section(attach);
        client.getRootWidget().add(destination);
        attach.onClick(event -> {
            target.remove();
            destination.add(target);
        });
        assertStatuses(send(client, target, attach, target), "obsolete", "applied", "applied");
        assertEquals(1, calls.get());
    }

    /**
     * Detach and reattach preserves identity but does not resurrect an already consumed ID.
     */
    @Test
    public void resolvesReattachedTargetInOrder() {
        final Client client = new Client();
        final Button detach = new Button("Detach");
        final Button attach = new Button("Attach");
        final Button target = new Button("Target");
        final Section section = new Section(detach, attach, target);
        final AtomicInteger calls = new AtomicInteger();
        target.onClick(event -> calls.incrementAndGet());
        detach.onClick(event -> target.remove());
        attach.onClick(event -> section.add(target));
        client.getRootWidget().add(section);
        final JsonObject response = send(client, detach, target, attach, target);
        assertStatuses(response, "applied", "obsolete", "applied", "applied");
        assertEquals(1, calls.get());
        assertStatuses(send(client, detach, target, attach, target),
            "obsolete", "obsolete", "obsolete", "obsolete");
        assertEquals(1, calls.get());
    }

    /**
     * An exception stops the batch; a retry resolves remaining targets in the mutated tree.
     */
    @Test
    public void stopsAfterExceptionAndDoesNotReplayEarlierSideEffects() {
        final Client client = new Client();
        final Button first = new Button("Throw");
        final Button removed = new Button("Removed");
        final Button attached = new Button("Attached");
        final Section section = new Section(first, removed);
        final AtomicInteger calls = new AtomicInteger();
        removed.onClick(event -> calls.addAndGet(100));
        attached.onClick(event -> calls.incrementAndGet());
        first.onClick(event -> {
            calls.addAndGet(10);
            removed.remove();
            section.add(attached);
            throw new IllegalStateException("Failure after tree mutation");
        });
        client.getRootWidget().add(section);
        assertThrows(IllegalStateException.class, () -> send(client, first, removed, attached));
        assertEquals(10, calls.get());
        assertStatuses(send(client, first, removed, attached), "obsolete", "obsolete", "applied");
        assertEquals(11, calls.get());
    }

    /**
     * Actual HTTP batches observe replacement and permit subsequent synchronization.
     */
    @Test
    public void resolvesReplacementOverAuthenticatedHttp() throws Exception {
        final Button first = new Button("Replace");
        final Button old = new Button("Old");
        final Button replacement = new Button("Replacement");
        final Section holder = new Section(old);
        final AtomicInteger calls = new AtomicInteger();
        old.onClick(event -> calls.addAndGet(100));
        replacement.onClick(event -> calls.incrementAndGet());
        first.onClick(event -> {
            holder.removeAll();
            holder.add(replacement);
        });
        final Application app = new Application((root, context) -> {
            root.add(new Section(first));
            root.add(holder);
        });
        final var server = Server.start(app, new Options.Builder().setPort(0).build());
        try {
            final var browser = new ProtocolTestBrowser(server.getPort());
            final JsonObject channel = browser.create(null);
            final var response = browser.post(Map.of("action", "synchronize", "client",
                channel.get("id").stringValue(), "events", batch(first, old, replacement)),
                browser.headers(channel));
            assertEquals(200, response.statusCode());
            assertStatuses(ProtocolTestBrowser.json(response), "applied", "obsolete", "applied");
            assertEquals(1, calls.get());
            final var next = browser.post(Map.of("action", "synchronize", "client",
                channel.get("id").stringValue(), "events", "[]"), browser.headers(channel));
            assertEquals(200, next.statusCode());
            assertEquals("#3", ProtocolTestBrowser.json(next).get("lastEvent").stringValue());
        } finally {
            server.stop();
        }
    }

    /**
     * Revoking either the tab or transport session stops all remaining HTTP batch actions.
     */
    @Test
    public void stopsHttpBatchAfterRevocation() throws Exception {
        for (final boolean session : new boolean[]{false, true}) {
            final Button revoke = new Button("Revoke");
            final Button next = new Button("Forbidden");
            final AtomicInteger calls = new AtomicInteger();
            next.onClick(event -> calls.incrementAndGet());
            final Application app = new Application((root, context) -> {
                revoke.onClick(event -> {
                    if (session) {
                        context.browserSession.revoke();
                    } else {
                        context.tabContext.revoke();
                    }
                });
                root.add(new Section(revoke, next));
            });
            final var server = Server.start(app, new Options.Builder().setPort(0).build());
            try {
                final var browser = new ProtocolTestBrowser(server.getPort());
                final JsonObject channel = browser.create(null);
                final Map<String, String> request = Map.of("action", "synchronize", "client",
                    channel.get("id").stringValue(), "events", batch(revoke, next));
                final var response = browser.post(request, browser.headers(channel));
                assertEquals(session ? 403 : 200, response.statusCode());
                assertTrue(ProtocolTestBrowser.json(response)
                    .get(session ? "sessionExpired" : "tabContextExpired").booleanValue());
                browser.post(request, browser.headers(channel));
                assertEquals(0, calls.get());
            } finally {
                server.stop();
            }
        }
    }

    /**
     * Builds an ordered browser-local click sequence without relying on global widget IDs.
     */
    private static String batch(final Button... targets) {
        final JsonArray events = new JsonArray();
        for (int index = 0; index < targets.length; index++) {
            final JsonObject event = events.createObject();
            event.addString("id", "#" + (index + 1));
            event.addString("widget", targets[index].getId().toString());
            event.addString("type", "click");
            event.put("data", EventTestData.pointer());
        }
        return events.toString();
    }

    /**
     * Dispatches a complete batch through the production synchronization boundary.
     */
    private static JsonObject send(final Client client, final Button... targets) {
        final JsonObject response = new JsonObject();
        client.synchronize(Map.of("events", batch(targets)), response);
        return response;
    }

    /**
     * Asserts the exact ordered terminal result of every event.
     */
    private static void assertStatuses(final JsonObject response, final String... expected) {
        final JsonArray results = response.get("eventResults").arrayValueOrThrow();
        assertEquals(expected.length, results.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], results.get(index).objectValueOrThrow()
                .get("status").stringValue());
        }
    }
}
