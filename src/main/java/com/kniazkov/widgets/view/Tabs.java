/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SingleThreadModel;
import com.kniazkov.widgets.model.StringModel;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Keyed, manually activated tabs. Arrow keys/Home/End move header focus; Enter/Space selects.
 * Content is lazy and retained while hidden, including editor drafts. Removal/replacement
 * disposes it. An unavailable active tab selects the first available key in display order.
 */
public final class Tabs extends NavigationPanels {
    /**
     * Owned validated selection model.
     */
    private final Model<String> selection = new SingleThreadModel<>() {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public String getData() {
            return Tabs.this.selectedKeys().stream().findFirst().orElse("");
        }

        @Override
        public boolean setData(final String data) {
            return data != null && Tabs.this.selectKeys(data.isEmpty() ? Set.of() : Set.of(data));
        }

        @Override
        public Model<String> deriveWithData(final String data) {
            return new StringModel(data);
        }
    };

    /**
     * Creates an empty tab list. Set an accessible name when several lists share a page.
     */
    public Tabs() {
        super(true, false);
        this.publish();
    }

    /**
     * @param key stable unique key
     * @param title visible header
     * @param content lazy owned content, retained until removal or replacement
     */
    public void addTab(final String key, final String title,
            final Supplier<? extends BlockWidget<?>> content) {
        this.addEntry(key, title, content);
    }

    /**
     * @return writable selection by key; empty only when no available tabs remain
     */
    public Model<String> getSelectedKeyModel() {
        return this.selection;
    }

    @Override
    protected void selectionChanged() {
        this.selection.notifyListeners();
    }
}
