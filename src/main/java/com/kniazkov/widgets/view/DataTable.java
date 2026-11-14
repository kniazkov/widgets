/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.protocol.SetDataTable;
import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A provider-backed native table with stable keyed row selection.
 */
public final class DataTable extends BlockWidget<TableStyle> implements HasDisabledState {
    /**
     * Immutable schema for one visible column. Captions and sort keys are independent.
     * @param key stable cell key
     * @param caption visible header
     * @param sortable whether it can be sent as a provider sort key
     */
    public record DataColumn(String key, String caption, boolean sortable) {
        /**
         * Validates the visible header and stable identity.
         */
        public DataColumn {
            if (Objects.requireNonNull(key).isBlank() || key.length() > 96
                    || Objects.requireNonNull(caption).isBlank() || caption.length() > 160) {
                throw new IllegalArgumentException("Invalid data table column");
            }
        }
    }

    /**
     * Localized table chrome (column titles and cell values belong to the application).
     * @param filter filter field label
     * @param choose selection column heading and checkbox prefix
     * @param previous previous page label
     * @param next next page label
     * @param retry retry label
     * @param loading loading state
     * @param empty empty result
     * @param error generic provider error
     * @param page page indicator prefix
     */
    public record Messages(String filter, String choose, String previous, String next,
                           String retry, String loading, String empty, String error, String page) {
        /**
         * Default English messages.
         */
        public static final Messages ENGLISH = new Messages("Filter", "Select",
            "Previous", "Next", "Retry", "Loading rows…", "No matching rows.",
            "Rows unavailable.", "Page");

        /**
         * Validates each label before publication.
         */
        public Messages {
            for (final String label : List.of(filter, choose, previous, next, retry, loading,
                    empty, error, page)) {
                if (label.isBlank() || label.length() > 160) {
                    throw new IllegalArgumentException("Invalid table message");
                }
            }
        }
    }

    /**
     * Page chrome and stable column order.
     */
    private final String caption;
    /**
     * Column schema.
     */
    private final List<DataColumn> columns;
    /**
     * Application provider.
     */
    private final DataProvider provider;
    /**
     * Localized labels.
     */
    private final Messages messages;
    /**
     * Keys retained across the current page and its siblings.
     */
    private final Set<String> selected = new LinkedHashSet<>();
    /**
     * Current requested query.
     */
    private DataProvider.Query query;
    /**
     * Application callback, invoked with immutable snapshots on selection changes.
     */
    private Controller<Set<String>> selectionChanged = keys -> { };
    /**
     * Latest accepted visible rows.
     */
    private List<DataProvider.DataRow> rows = List.of();
    /**
     * Filtered result count.
     */
    private int total;
    /**
     * State code for browser rendering.
     */
    private String state = "idle";
    /**
     * Request identity, invalidated before sending a cancellation hint.
     */
    private CompletableFuture<DataProvider.Page> pending;

    /**
     * Builds a bounded table with default labels.
     * @param caption native table caption
     * @param columns ordered schema
     * @param pageSize maximum visible rows (1–32)
     * @param provider page source
     */
    public DataTable(final String caption, final List<DataColumn> columns, final int pageSize,
                     final DataProvider provider) {
        this(caption, columns, pageSize, provider, Messages.ENGLISH);
    }

    /**
     * Builds a bounded table with application-language labels.
     * @param caption native table caption
     * @param columns ordered schema
     * @param pageSize maximum visible rows (1–32)
     * @param provider page source
     * @param messages localized chrome
     */
    @SuppressWarnings("this-escape")
    public DataTable(final String caption, final List<DataColumn> columns, final int pageSize,
                     final DataProvider provider, final Messages messages) {
        super(TableStyle.DEFAULT);
        this.caption = Objects.requireNonNull(caption);
        this.columns = List.copyOf(columns);
        if (caption.isBlank() || caption.length() > 160 || this.columns.isEmpty()
                || this.columns.size() > 8
                || this.columns.stream().map(DataColumn::key).distinct().count()
                    != this.columns.size()) {
            throw new IllegalArgumentException("Invalid table schema");
        }
        this.provider = Objects.requireNonNull(provider);
        this.messages = Objects.requireNonNull(messages);
        this.query = new DataProvider.Query("", "", false, 0, pageSize);
        this.subscribeToEvent(Event.DATA_TABLE);
        this.onRetire(() -> {
            this.cancelPending();
            this.selectionChanged = keys -> { };
        });
        this.publish();
    }

    @Override
    public String getType() {
        return "data table";
    }

    /**
     * @return latest requested filter, sort and page
     */
    public DataProvider.Query getQuery() {
        return this.query;
    }

    /**
     * @return latest rendered rows, never fetching more synchronously
     */
    public List<DataProvider.DataRow> getRows() {
        return this.rows;
    }

    /**
     * @return immutable selected keys, including keys from another page
     */
    public Set<String> getSelectedKeys() {
        return Set.copyOf(this.selected);
    }

    /**
     * Registers a callback after real changes to the selected keys.
     * @param callback selection listener owned by this table
     */
    public void onSelect(final Controller<Set<String>> callback) {
        this.requireUsable();
        this.selectionChanged = Objects.requireNonNull(callback);
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        this.requireUsable();
        if (!this.selected.isEmpty()) {
            this.selected.clear();
            this.selectionChanged.handleEvent(this.getSelectedKeys());
            this.publish();
        }
    }

    /**
     * Sets a filter and returns to page one. Empty filter asks the provider for all rows.
     * @param value filter string
     */
    public void setFilter(final String value) {
        this.changeQuery(new DataProvider.Query(value, this.query.sortKey(),
            this.query.descending(), 0, this.query.size()));
    }

    /**
     * Sets provider ordering and returns to page one.
     * @param key stable sortable column key or empty for default order
     * @param descending sort descending
     */
    public void setSort(final String key, final boolean descending) {
        if (!key.isEmpty() && !this.sortable(key)) {
            throw new IllegalArgumentException("Unknown sortable table column");
        }
        this.changeQuery(new DataProvider.Query(this.query.filter(), key, descending, 0,
            this.query.size()));
    }

    /**
     * Selects a zero-based page of the current query.
     * @param index zero-based page index
     */
    public void setPage(final int index) {
        if (index >= this.pageCount()) {
            throw new IllegalArgumentException("Page outside table result");
        }
        this.changeQuery(new DataProvider.Query(this.query.filter(), this.query.sortKey(),
            this.query.descending(), index, this.query.size()));
    }

    /**
     * Reloads from the provider without losing selected row keys.
     */
    public void reload() {
        this.requireUsable();
        this.getRootWidget().ifPresent(this::request);
    }

    /**
     * Replaces one currently visible record without discarding selections or changing its order.
     * Update the backing data separately; a later provider refresh can replace this snapshot.
     * @param row record with existing stable key
     * @return whether a visible row was updated
     */
    public boolean updateRow(final DataProvider.DataRow row) {
        this.requireUsable();
        Objects.requireNonNull(row);
        if (!this.visible(row.key())) {
            return false;
        }
        this.rows = this.rows.stream().map(old -> old.key().equals(row.key()) ? row : old).toList();
        this.publish();
        return true;
    }

    /**
     * Removes an authoritative deleted key from the visible page and any selection. Persist
     * deletion in the application first and reload to reconcile page boundaries if needed.
     * @param key deleted stable key
     */
    public void removeRow(final String key) {
        this.requireUsable();
        if (this.selected.remove(key)) {
            this.selectionChanged.handleEvent(this.getSelectedKeys());
        }
        if (this.visible(key)) {
            this.rows = this.rows.stream().filter(row -> !row.key().equals(key)).toList();
            this.total--;
        }
        this.publish();
    }

    /**
     * Validates an already type-checked browser command in the current table context.
     * @param action admitted action name
     * @param value column, filter or row key
     * @return whether the command targets current state
     */
    public boolean acceptsCommand(final String action, final String value) {
        return switch (action) {
            case "load" -> value.isEmpty() && ("idle".equals(this.state)
                || "error".equals(this.state));
            case "filter" -> value.length() <= 120;
            case "sort" -> this.sortable(value);
            case "previous" -> value.isEmpty() && this.query.index() > 0
                && !"loading".equals(this.state);
            case "next" -> value.isEmpty() && this.query.index() + 1 < this.pageCount()
                && !"loading".equals(this.state);
            case "row" -> value.length() <= 96 && this.visible(value)
                && !"loading".equals(this.state);
            default -> false;
        };
    }

    /**
     * Applies a validated table interaction.
     * @param action admitted command
     * @param value captured text/key
     * @param checked desired row selection
     */
    public void command(final String action, final String value, final boolean checked) {
        switch (action) {
            case "load" -> this.reload();
            case "filter" -> this.setFilter(value);
            case "sort" -> this.setSort(value,
                value.equals(this.query.sortKey()) && !this.query.descending());
            case "previous" -> this.setPage(this.query.index() - 1);
            case "next" -> this.setPage(this.query.index() + 1);
            case "row" -> {
                final boolean changed = checked
                    ? this.selected.size() < 512 && this.selected.add(value)
                    : this.selected.remove(value);
                if (changed) {
                    this.selectionChanged.handleEvent(this.getSelectedKeys());
                }
                this.publish();
            }
            default -> throw new IllegalArgumentException("Unknown table action");
        }
    }

    /**
     * @param key column identifier
     * @return whether provider sorting uses this key
     */
    private boolean sortable(final String key) {
        return this.columns.stream().anyMatch(column ->
            column.sortable() && column.key().equals(key));
    }

    /**
     * @param key row identifier
     * @return whether present in this snapshot
     */
    private boolean visible(final String key) {
        return this.rows.stream().anyMatch(row -> row.key().equals(key));
    }

    /**
     * @return page count, keeping the empty first page valid
     */
    private int pageCount() {
        return (int) Math.max(1L, (this.total + (long) this.query.size() - 1)
            / this.query.size());
    }

    /**
     * @param replacement query to dispatch
     */
    private void changeQuery(final DataProvider.Query replacement) {
        this.requireUsable();
        this.query = replacement;
        this.reload();
    }

    /**
     * Cancels and invalidates any old request.
     */
    private void cancelPending() {
        final var old = this.pending;
        this.pending = null;
        if (old != null) {
            old.cancel(true);
        }
    }

    /**
     * Publishes loading state, then invokes the provider outside its eventual callback.
     * @param root live owner to serialize completion with browser interaction
     */
    private void request(final RootWidget root) {
        this.cancelPending();
        this.state = "loading";
        this.publish();
        try {
            final var future = Objects.requireNonNull(this.provider.load(this.query));
            this.pending = future;
            final WeakReference<RootWidget> owner = new WeakReference<>(root);
            final WeakReference<DataTable> widget = new WeakReference<>(this);
            future.whenComplete((result, failure) -> {
                final RootWidget page = owner.get();
                final DataTable table = widget.get();
                if (page != null && table != null) {
                    page.execute(() -> table.finish(page, future, failure == null ? result : null));
                }
            });
        } catch (final RuntimeException failure) {
            this.state = "error";
            this.publish();
        }
    }

    /**
     * Applies only the currently owned request; tolerates best-effort cancellation.
     * @param root original page
     * @param future request identity
     * @param result page, or null on error
     */
    private void finish(final RootWidget root, final CompletableFuture<DataProvider.Page> future,
                        final DataProvider.Page result) {
        if (this.pending != future) {
            return;
        }
        this.pending = null;
        if (this.isRetired()) {
            return;
        }
        if (this.getRootWidget().orElse(null) != root) {
            this.state = "idle";
            this.publish();
            return;
        }
        if (result == null || result.rows().size() > this.query.size()) {
            this.state = "error";
        } else {
            this.total = result.total();
            if (this.query.index() >= this.pageCount()) {
                this.setPage(this.pageCount() - 1);
                return;
            }
            this.rows = result.rows();
            this.state = "ready";
        }
        this.publish();
    }

    /**
     * Sends the bounded current snapshot, with only visible rows as cells.
     */
    private void publish() {
        final JsonObject data = new JsonObject();
        data.addString("caption", this.caption);
        data.addString("state", this.state);
        data.addString("filter", this.query.filter());
        data.addString("sort", this.query.sortKey());
        data.addBoolean("descending", this.query.descending());
        data.addNumber("index", this.query.index());
        data.addNumber("pages", this.pageCount());
        data.addNumber("total", this.total);
        data.addNumber("selected", this.selected.size());
        final JsonObject labels = data.createObject("labels");
        labels.addString("filter", this.messages.filter());
        labels.addString("choose", this.messages.choose());
        labels.addString("previous", this.messages.previous());
        labels.addString("next", this.messages.next());
        labels.addString("retry", this.messages.retry());
        labels.addString("loading", this.messages.loading());
        labels.addString("empty", this.messages.empty());
        labels.addString("error", this.messages.error());
        labels.addString("page", this.messages.page());
        final var schema = data.createArray("columns");
        for (final DataColumn column : this.columns) {
            final var cell = new JsonObject();
            cell.addString("key", column.key());
            cell.addString("caption", column.caption());
            cell.addBoolean("sortable", column.sortable());
            schema.addElement(cell);
        }
        final var contents = data.createArray("rows");
        for (final DataProvider.DataRow row : this.rows) {
            final var entry = new JsonObject();
            entry.addString("key", row.key());
            entry.addBoolean("checked", this.selected.contains(row.key()));
            final var cells = entry.createArray("cells");
            this.columns.forEach(column -> cells.addString(
                row.cells().getOrDefault(column.key(), "")));
            contents.addElement(entry);
        }
        this.pushUpdate(new SetDataTable(this.getId(), data));
    }
}
