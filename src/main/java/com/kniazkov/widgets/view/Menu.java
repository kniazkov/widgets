/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * A flat action menu anchored to a native button. Stable keys survive reordering; disabled
 * actions are skipped. Selection closes before invoking the callback, exactly once per opening.
 */
public final class Menu extends AnchoredOverlay {
    /**
     * Immutable menu configuration.
     */
    private List<Item> items = List.of();

    /**
     * @param anchor native trigger
     * @param name accessible menu name
     * @param items ordered immutable actions
     */
    public Menu(final Button anchor, final String name, final List<Item> items) {
        super(anchor);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Menu needs an accessible name");
        }
        this.setAccessibleName(name);
        this.setItems(items);
    }

    /**
     * @param items replacement configuration; unique nonblank keys, up to 256 actions
     */
    public void setItems(final List<Item> items) {
        this.requireUsable();
        final List<Item> copy = List.copyOf(items);
        if (copy.size() > 256 || copy.stream().map(Item::key).distinct().count() != copy.size()) {
            throw new IllegalArgumentException("Menu keys must be unique; at most 256 items");
        }
        this.items = copy;
        this.invalidateOverlayCommands();
        this.publishOverlay();
    }

    /**
     * @return immutable action snapshot
     */
    public List<Item> getItems() {
        return this.items;
    }

    /**
     * @param key candidate action
     * @return whether the action is currently enabled
     */
    public boolean acceptsKey(final String key) {
        return this.items.stream().anyMatch(item -> item.key().equals(key) && item.enabled());
    }

    /**
     * @param key validated key; closure precedes application code
     */
    void choose(final String key) {
        final Item item = this.items.stream().filter(value -> value.key().equals(key))
            .findFirst().orElseThrow();
        this.close();
        item.action().run();
    }

    @Override
    public String getType() {
        return "anchored menu";
    }

    @Override
    protected String overlayKind() {
        return "menu";
    }

    @Override
    protected JsonArray menuSnapshot() {
        final JsonArray array = new JsonArray();
        for (final Item item : this.items) {
            final JsonObject value = new JsonObject();
            value.addString("key", item.key());
            value.addString("label", item.label());
            value.addBoolean("enabled", item.enabled());
            array.addElement(value);
        }
        return array;
    }

    /**
     * One immutable action, identified independently of its label or position.
     * @param key unique key, at most 128 characters
     * @param label nonblank visible caption
     * @param enabled availability
     * @param action callback invoked once after close
     */
    public record Item(String key, String label, boolean enabled, Runnable action) {
        /**
         * Validates the complete action before it can replace a live configuration.
         */
        public Item {
            if (key == null || key.isBlank() || key.length() > 128
                    || label == null || label.isBlank()) {
                throw new IllegalArgumentException("Menu needs nonblank keys and labels");
            }
            Objects.requireNonNull(action, "action");
        }
    }
}
