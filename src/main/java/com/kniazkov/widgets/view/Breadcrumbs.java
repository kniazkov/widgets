/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;
import java.util.Objects;

/**
 * A labelled navigation landmark containing a wrapping path of real links.
 * The final item is current-page text, never an intercepted link. Long labels wrap,
 * including unbroken words; the complete path stays available to assistive technology.
 */
public final class Breadcrumbs extends Panel {
    /**
     * Immutable application path snapshot.
     */
    private List<Item> items = List.of();

    /**
     * @param name accessible navigation name
     * @param path path ending with the current item; empty clears the path
     */
    @SuppressWarnings("this-escape")
    public Breadcrumbs(final String name, final List<Item> path) {
        this.setAccessibleName(Objects.requireNonNull(name, "name"));
        this.setItems(path);
    }

    /**
     * Replaces the entire path and disposes old links.
     * @param path immutable-value items; every preceding item needs a nonblank href
     */
    public void setItems(final List<Item> path) {
        this.requireUsable();
        final List<Item> copy = List.copyOf(path);
        for (int index = 0; index + 1 < copy.size(); index++) {
            if (copy.get(index).href().isBlank()) {
                throw new IllegalArgumentException("An ancestor breadcrumb needs an href");
            }
        }
        while (this.getChildCount() > 0) {
            this.getChild(0).dispose();
        }
        this.items = copy;
        for (int index = 0; index < copy.size(); index++) {
            final Item item = copy.get(index);
            final Section row = new TextFlow();
            if (index + 1 == copy.size()) {
                row.add(new TextWidget(item.title()));
            } else {
                row.add(new Link(item.title(), item.href()));
            }
            super.add(row);
        }
    }

    /**
     * @return immutable path snapshot
     */
    public List<Item> getItems() {
        return this.items;
    }

    @Override
    public String getType() {
        return "breadcrumbs";
    }

    /**
     * One path segment. The final item's href is ignored and may be empty.
     * @param title nonblank visible title
     * @param href ancestor URL, using the ordinary Link URL policy
     */
    public record Item(String title, String href) {
        /**
         * Validates labels and rejects null URLs before changing the live path.
         */
        public Item {
            if (Objects.requireNonNull(title, "title").isBlank()) {
                throw new IllegalArgumentException("A breadcrumb title must not be blank");
            }
            Objects.requireNonNull(href, "href");
        }
    }
}
