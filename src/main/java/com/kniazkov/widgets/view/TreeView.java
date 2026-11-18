/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ReadOnlyModel;
import com.kniazkov.widgets.protocol.SetTreeView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Single-selection tree with lazy branches. Mutate on the owning page thread;
 * provider completions are serialized through the page root. Keys are unique across the tree.
 */
public final class TreeView extends BlockWidget<PanelStyle> implements HasDisabledState {
    /**
     * Provider descriptor. A branch may resolve to an empty list.
     * @param key stable global identity
     * @param label visible plain text
     * @param branch whether children can be requested
     */
    public record Node(String key, String label, boolean branch) {
        /**
         * Rejects unusable row identities and captions.
         */
        public Node {
            if (key == null || key.isBlank() || key.length() > 128 || label == null
                    || label.isBlank()) {
                throw new IllegalArgumentException("Expected a stable key and a caption");
            }
        }
    }

    /**
     * Application-localized status labels.
     * @param loading pending branch
     * @param empty empty tree or branch
     * @param error failed load; collapse and expand to retry
     */
    public record Messages(String loading, String empty, String error) {
        /**
         * Requires plain-text labels.
         */
        public Messages {
            List.of(loading, empty, error).forEach(Objects::requireNonNull);
        }
    }

    /**
     * An entry exists only after its parent has provided it.
     */
    private static final class Entry {
        /**
         * Display descriptor.
         */
        private Node node;
        /**
         * Current parent, null for roots.
         */
        private Entry parent;
        /**
         * Materialized children in display order.
         */
        private final List<Entry> children = new ArrayList<>();
        /**
         * Whether the first successful load has completed.
         */
        private boolean loaded;
        /**
         * Whether display should announce a failed request.
         */
        private boolean failed;
        /**
         * Unique token for the current asynchronous request.
         */
        private Object request;

        /**
         * @param node display descriptor
         * @param parent owning branch
         */
        Entry(final Node node, final Entry parent) {
            this.node = node;
            this.parent = parent;
        }
    }

    /**
     * Materialized keys, including collapsed cached children.
     */
    private final Map<String, Entry> entries = new HashMap<>();
    /**
     * Ordered root rows.
     */
    private final List<Entry> roots = new ArrayList<>();
    /**
     * Keys with visible child lists.
     */
    private final Set<String> expanded = new LinkedHashSet<>();
    /**
     * Selected key, empty when cleared.
     */
    private String selected = "";
    /**
     * Asynchronous source of direct children.
     */
    private final Function<String, ? extends CompletionStage<List<Node>>> provider;
    /**
     * Localized branch status messages.
     */
    private final Messages messages;
    /**
     * Observable selection.
     */
    private final Model<String> selection = observe(() -> this.selected);
    /**
     * Observable immutable expansion state.
     */
    private final Model<Set<String>> expansion = observe(() -> Set.copyOf(this.expanded));

    /**
     * @param nodes initial roots; no provider call is made until expansion
     * @param provider nonblocking source of direct children, called once per uncached expansion
     * @param messages localized status text
     */
    @SuppressWarnings("this-escape")
    public TreeView(final List<Node> nodes,
            final Function<String, ? extends CompletionStage<List<Node>>> provider,
            final Messages messages) {
        super(PanelStyle.DEFAULT);
        this.provider = Objects.requireNonNull(provider);
        this.messages = Objects.requireNonNull(messages);
        this.install(null, nodes);
        this.subscribeToEvent(Event.TREE_VIEW);
        this.onRetire(() -> {
            this.entries.clear();
            this.roots.clear();
            this.expanded.clear();
            this.selected = "";
        });
        this.publish();
    }

    /**
     * @param value current authoritative value
     * @param <T> snapshot type
     * @return observable view
     */
    private static <T> Model<T> observe(final Supplier<T> value) {
        return new ReadOnlyModel<>() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public T getData() {
                return value.get();
            }
        };
    }

    @Override
    public String getType() {
        return "tree view";
    }

    /**
     * @return observable stable key; call select to change it
     */
    public Model<String> getSelectedKeyModel() {
        return this.selection;
    }

    /**
     * @return observable immutable expanded keys; call expand or collapse to change them
     */
    public Model<Set<String>> getExpandedKeysModel() {
        return this.expansion;
    }

    /**
     * @param key materialized key or empty string to clear
     */
    public void select(final String key) {
        this.requireUsable();
        if (!key.isEmpty()) {
            this.entry(key);
        }
        if (!this.selected.equals(key)) {
            this.selected = key;
            this.publish();
            this.selection.notifyListeners();
        }
    }

    /**
     * @param parent target branch, null for roots
     * @param nodes fresh provider result; validate the entire batch before installing anything
     */
    private void install(final Entry parent, final List<Node> nodes) {
        final List<Node> batch = List.copyOf(nodes);
        final Set<String> keys = new LinkedHashSet<>();
        for (final Node node : batch) {
            if (!keys.add(node.key()) || this.entries.containsKey(node.key())) {
                throw new IllegalArgumentException("Duplicate or cyclic tree key: " + node.key());
            }
        }
        for (final Node node : batch) {
            final Entry child = new Entry(node, parent);
            this.entries.put(node.key(), child);
            this.children(parent).add(child);
        }
    }

    /**
     * @param key materialized branch; loads only direct children and only when mounted
     */
    public void expand(final String key) {
        final Entry entry = this.entry(key);
        if (!entry.node.branch() || this.expanded.contains(key)) {
            return;
        }
        final RootWidget root = this.getRootWidget().orElseThrow();
        this.expanded.add(key);
        entry.failed = false;
        final Object token = new Object();
        if (!entry.loaded) {
            entry.request = token;
        }
        this.publish();
        this.expansion.notifyListeners();
        if (entry.request != token) {
            return;
        }
        try {
            final var owner = new java.lang.ref.WeakReference<>(this);
            final var page = new java.lang.ref.WeakReference<>(root);
            this.provider.apply(key).whenComplete((nodes, failure) -> {
                final TreeView tree = owner.get();
                final RootWidget live = page.get();
                if (tree != null && live != null) {
                    live.execute(() -> tree.finish(key, token, failure == null ? nodes : null));
                }
            });
        } catch (final RuntimeException failure) {
            this.finish(key, token, null);
        }
    }

    /**
     * @param key requested branch
     * @param token unique expansion request
     * @param nodes result, null on provider failure
     */
    private void finish(final String key, final Object token, final List<Node> nodes) {
        final Entry entry = this.entries.get(key);
        if (entry == null || entry.request != token) {
            return;
        }
        entry.request = null;
        try {
            this.install(entry, nodes);
            entry.loaded = true;
        } catch (final RuntimeException failure) {
            entry.failed = true;
        }
        this.publish();
    }

    /**
     * Collapses a subtree, invalidating pending loads but retaining successful data and selection.
     * @param key materialized key
     */
    public void collapse(final String key) {
        final Entry entry = this.entry(key);
        if (this.expanded.contains(key)) {
            this.closeBranch(entry);
            this.publish();
            this.expansion.notifyListeners();
        }
    }

    /**
     * @param entry branch to close
     */
    private void closeBranch(final Entry entry) {
        if (this.expanded.remove(entry.node.key())) {
            entry.request = null;
            entry.children.forEach(this::closeBranch);
        }
    }

    /**
     * @param key materialized key
     * @param label new caption
     */
    public void rename(final String key, final String label) {
        final Entry entry = this.entry(key);
        entry.node = new Node(key, label, entry.node.branch());
        this.publish();
    }

    /**
     * Appends a subtree to a loaded branch or to the root list, preserving keyed selection.
     * @param key materialized node
     * @param parentKey destination key, empty for root
     */
    public void move(final String key, final String parentKey) {
        final Entry entry = this.entry(key);
        final Entry parent = parentKey.isEmpty() ? null : this.entry(parentKey);
        if (parent != null && !parent.loaded) {
            throw new IllegalArgumentException("Expected a loaded destination branch");
        }
        for (Entry ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == entry) {
                throw new IllegalArgumentException("Cannot move a node into its subtree");
            }
        }
        this.children(entry.parent).remove(entry);
        entry.parent = parent;
        this.children(parent).add(entry);
        this.publish();
    }

    /**
     * Removes a complete subtree and clears selection if it was inside it.
     * @param key materialized key
     */
    public void remove(final String key) {
        final Entry entry = this.entry(key);
        this.children(entry.parent).remove(entry);
        final int count = this.expanded.size();
        this.forget(entry);
        final boolean clear = !this.selected.isEmpty() && !this.entries.containsKey(this.selected);
        if (clear) {
            this.selected = "";
        }
        this.publish();
        if (clear) {
            this.selection.notifyListeners();
        }
        if (this.expanded.size() != count) {
            this.expansion.notifyListeners();
        }
    }

    /**
     * @param entry removed subtree
     */
    private void forget(final Entry entry) {
        this.entries.remove(entry.node.key());
        this.expanded.remove(entry.node.key());
        entry.children.forEach(this::forget);
    }

    /**
     * @param parent parent entry or null for roots
     * @return ordered direct children
     */
    private List<Entry> children(final Entry parent) {
        return parent == null ? this.roots : parent.children;
    }

    /**
     * @param key stable key
     * @return materialized entry
     */
    private Entry entry(final String key) {
        this.requireUsable();
        final Entry entry = this.entries.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown tree key: " + key);
        }
        return entry;
    }

    /**
     * @param action browser operation
     * @param key target key
     * @return whether this operation may address the visible row
     */
    public boolean accepts(final String action, final String key) {
        final Entry entry = this.entries.get(key);
        if (!this.isInteractionEnabled() || entry == null) {
            return false;
        }
        for (Entry p = entry.parent; p != null; p = p.parent) {
            if (!this.expanded.contains(p.node.key())) {
                return false;
            }
        }
        return "select".equals(action) || (entry.node.branch()
            && ("open".equals(action) || "close".equals(action)));
    }

    /**
     * @param action validated browser operation
     * @param key target key
     */
    public void command(final String action, final String key) {
        if (this.accepts(action, key)) {
            switch (action) {
                case "open" -> this.expand(key);
                case "close" -> this.collapse(key);
                default -> this.select(key);
            }
        }
    }

    /**
     * Publishes the visible projection only.
     */
    private void publish() {
        final JsonObject snapshot = new JsonObject();
        final JsonArray rows = new JsonArray();
        this.roots.forEach(entry -> this.append(rows, entry));
        snapshot.addElement("rows", rows);
        snapshot.addString("selected", this.selected);
        snapshot.addString("empty", this.messages.empty());
        this.pushUpdate(new SetTreeView(this.getId(), snapshot));
    }

    /**
     * @param rows visible preorder rows
     * @param entry visible entry
     */
    private void append(final JsonArray rows, final Entry entry) {
        final JsonObject row = new JsonObject();
        row.addString("key", entry.node.key());
        row.addString("label", entry.node.label());
        row.addString("parent", entry.parent == null ? "" : entry.parent.node.key());
        row.addBoolean("branch", entry.node.branch());
        final boolean open = this.expanded.contains(entry.node.key());
        row.addBoolean("open", open);
        if (open && entry.children.isEmpty()) {
            row.addString("status", entry.failed ? this.messages.error()
                : entry.loaded ? this.messages.empty() : this.messages.loading());
        }
        rows.addElement(row);
        if (open) {
            entry.children.forEach(child -> this.append(rows, child));
        }
    }
}
