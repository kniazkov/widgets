/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.SetNavigation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shared lifecycle for keyed navigation panels. Use the concrete Tabs or Accordion API.
 * Content belongs to this widget: it is created lazily, retained while hidden, and disposed
 * on replacement/removal. Factories must return a new, unattached, usable block widget.
 * Like other view widgets, configuration is confined to the owning page thread.
 */
public abstract class NavigationPanels extends Panel {
    /**
     * Stable display order.
     */
    private final List<Entry> entries = new ArrayList<>();
    /**
     * Expanded keys, independent of display positions.
     */
    private Set<String> selected = Set.of();
    /**
     * Whether this is a tab list.
     */
    private final boolean tabs;
    /**
     * Whether multiple panels may be expanded.
     */
    private final boolean multiple;
    /**
     * Shared tab header row; accordion entries have individual rows.
     */
    private final TextFlow tabHeaders = new TextFlow();

    /**
     * @param tabs tab semantics instead of accordion semantics
     * @param multiple permit multiple expanded panels
     */
    @SuppressWarnings("this-escape")
    protected NavigationPanels(final boolean tabs, final boolean multiple) {
        this.tabs = tabs;
        this.multiple = multiple;
        if (tabs) {
            super.add(this.tabHeaders);
        }
        this.onRetire(() -> {
            this.entries.clear();
            this.selected = Set.of();
        });
    }

    /**
     * @return immutable keys in display order
     */
    public final List<String> getKeys() {
        return this.entries.stream().map(entry -> entry.key).toList();
    }

    /**
     * Adds a lazy panel. The first available tab is selected automatically.
     * @param key unique nonblank key
     * @param title visible and accessible header
     * @param content factory evaluated on first activation
     */
    protected final void addEntry(final String key, final String title,
            final Supplier<? extends BlockWidget<?>> content) {
        this.requireUsable();
        if (key == null || key.isBlank() || this.getKeys().contains(key)) {
            throw new IllegalArgumentException("Expected a unique nonblank navigation key");
        }
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        final Entry entry = new Entry(key, title, content);
        if (this.tabs && this.selected.isEmpty()) {
            entry.materialize();
        }
        final TextFlow headers = this.tabs ? this.tabHeaders : entry.headers;
        headers.add(entry.button);
        if (!this.tabs) {
            super.add(headers);
        }
        super.add(entry.panel);
        entry.panel.setAccessibleLabel(entry.button);
        entry.button.onClick(event -> {
            if (this.isInteractionEnabled() && entry.available()) {
                final Set<String> next = new LinkedHashSet<>(this.selected);
                if (this.tabs || !this.multiple) {
                    next.clear();
                }
                if (this.tabs || !this.selected.contains(key)) {
                    next.add(key);
                } else {
                    next.remove(key);
                }
                this.selectKeys(next);
            }
        });
        this.entries.add(entry);
        if (this.tabs && this.selected.isEmpty()) {
            this.selected = Set.of(key);
        }
        this.publish();
        this.selectionChanged();
    }

    /**
     * @return immutable selection snapshot
     */
    protected final Set<String> selectedKeys() {
        return Set.copyOf(this.selected);
    }

    /**
     * Invalid or unchanged selections return false and leave all state intact.
     * @param keys requested available keys; tabs require one unless none are available
     * @return whether the selection changed
     */
    protected final boolean selectKeys(final Set<String> keys) {
        if (this.isRetired() || keys == null || keys.equals(this.selected)
                || (!this.multiple && keys.size() > 1)
                || (this.tabs && keys.isEmpty()
                    && this.entries.stream().anyMatch(Entry::available))) {
            return false;
        }
        for (final String key : keys) {
            if (this.entries.stream().noneMatch(item -> item.key.equals(key) && item.available())) {
                return false;
            }
        }
        for (final Entry entry : this.entries) {
            if (keys.contains(entry.key)) {
                entry.materialize();
            }
        }
        this.selected = Set.copyOf(keys);
        this.publish();
        this.selectionChanged();
        return true;
    }

    /**
     * Notifies the concrete widget's public selection model after a committed change.
     */
    protected abstract void selectionChanged();

    /**
     * Permanently disposes a panel and its header. A removed active tab falls back to the
     * first available tab in current display order. Removing an unknown key is an error.
     * @param key panel key
     */
    public final void removeItem(final String key) {
        this.requireUsable();
        final Entry entry = this.entry(key);
        this.prepareFallback(entry);
        this.entries.remove(entry);
        this.reconcile();
        entry.button.dispose();
        entry.panel.dispose();
        if (!this.tabs) {
            entry.headers.dispose();
        }
    }

    /**
     * Reorders every key exactly once without rebuilding content or changing selection.
     * @param keys complete desired order
     */
    public final void reorder(final List<String> keys) {
        this.requireUsable();
        if (keys == null || keys.size() != this.entries.size()
                || new LinkedHashSet<>(keys).size() != keys.size()
                || !new LinkedHashSet<>(keys).equals(new LinkedHashSet<>(this.getKeys()))) {
            throw new IllegalArgumentException("Expected a permutation of all navigation keys");
        }
        final List<Entry> ordered = keys.stream().map(this::entry).toList();
        this.entries.clear();
        this.entries.addAll(ordered);
        this.publish();
    }

    /**
     * Replaces owned content. Expanded content is created before the old subtree is retired;
     * collapsed content remains lazy until opened again.
     * @param key panel key
     * @param factory new content factory
     */
    public final void replaceContent(final String key,
            final Supplier<? extends BlockWidget<?>> factory) {
        this.requireUsable();
        final Entry entry = this.entry(key);
        Objects.requireNonNull(factory, "factory");
        final BlockWidget<?> replacement = this.selected.contains(key)
            ? Objects.requireNonNull(factory.get(), "content") : null;
        if (replacement != null) {
            replacement.validateAttachment(entry.panel);
        }
        if (entry.content != null) {
            this.publish(key);
            entry.content.dispose();
        }
        entry.factory = factory;
        entry.content = replacement;
        if (replacement != null) {
            entry.panel.add(replacement);
        }
        this.publish();
    }

    /**
     * @param key panel key
     * @param visible false excludes the header and panel from interaction and selection
     */
    public final void setItemVisible(final String key, final boolean visible) {
        this.requireUsable();
        final Entry entry = this.entry(key);
        if (!visible) {
            this.prepareFallback(entry);
        } else if (this.tabs && this.selected.isEmpty() && entry.enabled) {
            entry.materialize();
        }
        entry.visible = visible;
        this.reconcile();
    }

    /**
     * @param key panel key
     * @param enabled false disables the header and excludes the panel from selection
     */
    public final void setItemEnabled(final String key, final boolean enabled) {
        this.requireUsable();
        final Entry entry = this.entry(key);
        if (!enabled) {
            this.prepareFallback(entry);
        } else if (this.tabs && this.selected.isEmpty() && entry.visible) {
            entry.materialize();
        }
        entry.enabled = enabled;
        this.reconcile();
    }

    /**
     * @param key panel key
     * @return matching owned entry
     */
    private Entry entry(final String key) {
        return this.entries.stream().filter(item -> item.key.equals(key)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown navigation key: " + key));
    }

    /**
     * Runs a fallible fallback factory before removing or hiding the current selection.
     * @param excluded entry about to become unavailable
     */
    private void prepareFallback(final Entry excluded) {
        if (this.tabs && this.selected.contains(excluded.key)) {
            this.entries.stream().filter(item -> item != excluded && item.available())
                .findFirst().ifPresent(Entry::materialize);
        }
    }

    /**
     * Removes unavailable selection and chooses a deterministic tab fallback.
     */
    private void reconcile() {
        final Set<String> next = new LinkedHashSet<>();
        for (final Entry entry : this.entries) {
            if (entry.available() && this.selected.contains(entry.key)) {
                next.add(entry.key);
            }
        }
        if (this.tabs && next.isEmpty()) {
            this.entries.stream().filter(Entry::available).findFirst().ifPresent(entry -> {
                entry.materialize();
                next.add(entry.key);
            });
        }
        final boolean changed = !next.equals(this.selected);
        this.selected = Set.copyOf(next);
        this.publish();
        if (changed) {
            this.selectionChanged();
        }
    }

    /**
     * Publishes native semantics before hiding/removing a focused control, allowing fallback.
     */
    protected final void publish() {
        this.publish("");
    }

    /**
     * @param replacing key whose focused content is about to be retired, or empty
     */
    private void publish(final String replacing) {
        final JsonObject state = new JsonObject();
        state.addString("replacing", replacing);
        state.addString("kind", this.tabs ? "tabs" : "accordion");
        state.addString("headers", this.tabs ? this.tabHeaders.getId().toString() : "");
        final JsonArray items = new JsonArray();
        for (final Entry entry : this.entries) {
            final JsonObject item = new JsonObject();
            item.addString("key", entry.key);
            item.addString("button", entry.button.getId().toString());
            item.addString("panel", entry.panel.getId().toString());
            item.addString("headers", (this.tabs ? this.tabHeaders : entry.headers)
                .getId().toString());
            item.addBoolean("selected", this.selected.contains(entry.key));
            item.addBoolean("visible", entry.visible);
            item.addBoolean("enabled", entry.enabled);
            items.addElement(item);
        }
        state.addElement("items", items);
        this.pushUpdate(new SetNavigation(this.getId(), state));
        for (final Entry entry : this.entries) {
            entry.button.setHiddenFlag(!entry.visible);
            entry.button.setDisabledFlag(!entry.enabled);
            entry.panel.setHiddenFlag(!this.selected.contains(entry.key));
            if (!this.tabs) {
                entry.headers.setHiddenFlag(!entry.visible);
            }
        }
    }

    /**
     * Owned header, content host and deferred factory.
     */
    private static final class Entry {
        /**
         * Stable application key.
         */
        private final String key;
        /**
         * Native activation control.
         */
        private final Button button;
        /**
         * Accordion header row.
         */
        private final TextFlow headers = new TextFlow();
        /**
         * Stable labelled content host.
         */
        private final Panel panel = new Panel();
        /**
         * Deferred content factory.
         */
        private Supplier<? extends BlockWidget<?>> factory;
        /**
         * Materialized content, or null before first expansion.
         */
        private BlockWidget<?> content;
        /**
         * Header visibility.
         */
        private boolean visible = true;
        /**
         * Header availability.
         */
        private boolean enabled = true;

        /**
         * @param key stable key
         * @param title header caption
         * @param factory deferred content
         */
        private Entry(final String key, final String title,
                final Supplier<? extends BlockWidget<?>> factory) {
            this.key = key;
            this.button = new Button(title);
            this.factory = factory;
        }

        /**
         * @return whether the entry can be activated
         */
        private boolean available() {
            return this.visible && this.enabled;
        }

        /**
         * Creates content at most once until explicitly replaced.
         */
        private void materialize() {
            if (this.content == null) {
                final BlockWidget<?> widget = Objects.requireNonNull(this.factory.get(), "content");
                this.panel.add(widget);
                this.content = widget;
            }
        }
    }
}
