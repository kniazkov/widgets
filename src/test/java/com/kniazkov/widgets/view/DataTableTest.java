/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

/**
 * Provider query, result lifetime and keyed table-model tests.
 */
public class DataTableTest {
    /**
     * Shared column schema with distinct display caption and sort key.
     */
    private static final List<DataTable.DataColumn> COLUMNS = List.of(
        new DataTable.DataColumn("title", "Same caption", true),
        new DataTable.DataColumn("value", "Same caption", false));
    /**
     * A stable row even if a caption is repeated.
     */
    private static final DataProvider.DataRow FIRST = row("a", "Same");
    /**
     * Same caption, different key.
     */
    private static final DataProvider.DataRow SECOND = row("b", "Same");

    /**
     * Queries carry stable keys, bounded pages and the current filter.
     */
    @Test
    public void sendsServerSortFilterAndPageQueries() {
        final List<DataProvider.Query> requests = new ArrayList<>();
        final DataTable table = new DataTable("Orders", COLUMNS, 1, query -> {
            requests.add(query);
            return CompletableFuture.completedFuture(new DataProvider.Page(
                List.of(query.index() == 0 ? FIRST : SECOND), 2));
        });
        final RootWidget root = new RootWidget(table);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        table.handleEvent("data table command", command("load", "", false));
        assertEquals(List.of(FIRST), table.getRows());
        table.handleEvent("data table command", command("sort", "title", false));
        assertEquals("title", table.getQuery().sortKey());
        table.handleEvent("data table command", command("sort", "title", false));
        assertTrue(table.getQuery().descending());
        table.handleEvent("data table command", command("filter", "archive", false));
        table.handleEvent("data table command", command("next", "", false));
        assertEquals(new DataProvider.Query("archive", "title", true, 1, 1),
            requests.getLast());
        assertEquals(List.of(SECOND), table.getRows());
        assertEquals(2, state(sandbox).get("total").intValue());
        assertThrows(IllegalArgumentException.class, () -> table.setSort("Same caption", false));
        assertThrows(IllegalArgumentException.class, () -> table.setPage(2));
        assertEquals("invalidPayload", Event.DATA_TABLE.rejectionReason(table,
            command("sort", "value", false)).orElseThrow());
        root.closePage();
    }

    /**
     * Selection survives searching and editing; deletion removes only the affected identity.
     */
    @Test
    public void keepsSelectionOnEmptyResultAndClearsDeletedKey() {
        final DataTable table = new DataTable("Orders", COLUMNS, 5, query ->
            CompletableFuture.completedFuture(query.filter().isEmpty()
                ? new DataProvider.Page(List.of(FIRST, SECOND), 2)
                : new DataProvider.Page(List.of(), 0)));
        final RootWidget root = new RootWidget(table);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        table.reload();
        table.handleEvent("data table command", command("row", "b", true));
        table.setFilter("active only");
        assertTrue(table.getRows().isEmpty());
        assertEquals(0, state(sandbox).get("total").intValue());
        assertEquals(Set.of("b"), table.getSelectedKeys());
        table.setFilter("");
        assertEquals(List.of(FIRST, SECOND), table.getRows());
        final var changed = row("b", "Edited value");
        assertTrue(table.updateRow(changed));
        assertEquals(List.of(FIRST, changed), table.getRows());
        assertEquals(Set.of("b"), table.getSelectedKeys());
        assertFalse(table.updateRow(row("missing", "Nobody")));
        table.removeRow("b");
        assertEquals(List.of(FIRST), table.getRows());
        assertTrue(table.getSelectedKeys().isEmpty());
        assertEquals("invalidPayload", Event.DATA_TABLE.rejectionReason(table,
            command("row", "b", true)).orElseThrow());
        root.closePage();
    }

    /**
     * Older responses cannot apply even when the provider ignores cancellation.
     */
    @Test
    public void ignoresLateResponsesAndAllowsRetryAfterError() {
        final List<DelayedPage> futures = new ArrayList<>();
        final DataTable table = new DataTable("Orders", COLUMNS, 5, query -> {
            final DelayedPage result = new DelayedPage();
            futures.add(result);
            return result;
        });
        final RootWidget root = new RootWidget(table);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        table.setFilter("slow");
        table.setFilter("fast");
        assertEquals(1, futures.getFirst().cancels);
        futures.getLast().complete(new DataProvider.Page(List.of(SECOND), 1));
        futures.getFirst().complete(new DataProvider.Page(List.of(FIRST), 1));
        assertEquals(List.of(SECOND), table.getRows());

        table.setFilter("error");
        futures.getLast().completeExceptionally(new IllegalStateException("unavailable"));
        assertEquals("error", state(sandbox).get("state").stringValue());
        assertEquals(List.of(SECOND), table.getRows());
        table.handleEvent("data table command", command("load", "", false));
        futures.getLast().complete(new DataProvider.Page(List.of(), 0));
        assertEquals("ready", state(sandbox).get("state").stringValue());
        assertTrue(table.getRows().isEmpty());

        table.reload();
        table.dispose();
        assertEquals(1, futures.getLast().cancels);
        root.closePage();
    }

    /**
     * Bad provider page and unsupported columns fail before state changes.
     */
    @Test
    public void validatesBoundsAndProviderResults() {
        assertThrows(IllegalArgumentException.class, () ->
            new DataTable("Orders", COLUMNS, 33, query -> null));
        assertThrows(IllegalArgumentException.class, () ->
            new DataProvider.Page(List.of(FIRST, FIRST), 2));
        assertThrows(IllegalArgumentException.class, () ->
            new DataProvider.Page(List.of(SECOND), 0));
        final DataTable table = new DataTable("Orders", COLUMNS, 1, query ->
            CompletableFuture.completedFuture(new DataProvider.Page(List.of(FIRST, SECOND), 2)));
        final RootWidget root = new RootWidget(table);
        final WidgetSandbox<RootWidget> sandbox = WidgetSandbox.open(root);
        sandbox.clearUpdates();
        table.reload();
        assertEquals("error", state(sandbox).get("state").stringValue());
        assertTrue(table.getRows().isEmpty());
        root.closePage();
    }

    /**
     * @param key row key
     * @param title display caption
     * @return projected row
     */
    private static DataProvider.DataRow row(final String key, final String title) {
        return new DataProvider.DataRow(key, Map.of("title", title, "value", "Example"));
    }

    /**
     * @param action browser command
     * @param value string payload
     * @param selected selected flag
     * @return normal event payload
     */
    private static JsonObject command(final String action, final String value,
                                      final boolean selected) {
        final JsonObject result = new JsonObject();
        result.addString("action", action);
        result.addString("value", value);
        result.addBoolean("checked", selected);
        return result;
    }

    /**
     * @param sandbox widget serialized stream
     * @return current table snapshot
     */
    private static JsonObject state(final WidgetSandbox<RootWidget> sandbox) {
        return sandbox.drainUpdates().stream().filter(update ->
            "set data table".equals(update.get("action").stringValue()))
            .reduce((old, update) -> update).orElseThrow()
            .get("snapshot").toJsonObject().orElseThrow();
    }

    /**
     * Provider may be unable to cancel its network call.
     */
    private static final class DelayedPage extends CompletableFuture<DataProvider.Page> {
        /**
         * Received cancellation attempts.
         */
        private int cancels;

        @Override
        public boolean cancel(final boolean interrupt) {
            this.cancels++;
            return false;
        }
    }
}
