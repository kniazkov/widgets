/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.ReadOnlyModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.SingleThreadModel;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Collapsible sections with native buttons and labelled regions. Enter/Space toggles;
 * Up/Down/Home/End moves between available headers. Content is retained while collapsed.
 */
public final class Accordion extends NavigationPanels {
    /**
     * Validated immutable expanded-key snapshots.
     */
    private final Model<Set<String>> expanded = new SingleThreadModel<>() {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Set<String> getData() {
            return Accordion.this.selectedKeys();
        }

        @Override
        public boolean setData(final Set<String> data) {
            return Accordion.this.selectKeys(data);
        }

        @Override
        public Model<Set<String>> deriveWithData(final Set<String> data) {
            return ReadOnlyModel.create(Set.copyOf(data));
        }
    };

    /**
     * @param multiple true permits several open sections; false permits zero or one
     */
    public Accordion(final boolean multiple) {
        super(false, multiple);
        this.publish();
    }

    /**
     * @param key stable unique key
     * @param title visible header
     * @param content lazy owned content
     */
    public void addSection(final String key, final String title,
            final Supplier<? extends BlockWidget<?>> content) {
        this.addEntry(key, title, content);
    }

    /**
     * @return writable model of immutable expanded keys
     */
    public Model<Set<String>> getExpandedKeysModel() {
        return this.expanded;
    }

    @Override
    protected void selectionChanged() {
        this.expanded.notifyListeners();
    }
}
