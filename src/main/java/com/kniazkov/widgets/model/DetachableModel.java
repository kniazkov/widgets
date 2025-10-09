/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.view.RootWidget;
import java.util.Optional;

public class DetachableModel<T> extends Model<T> {
    private final Model<T> base;

    public DetachableModel(final RootWidget root, final Model<T> base) {
        this.base = base;
        final Listener<T> listener = this::notifyListeners;
        base.addListener(listener);
        root.onClose(() -> {
            base.removeListener(listener);
        });
    }

    @Override
    public boolean isValid() {
        return this.base.isValid();
    }

    @Override
    protected Optional<T> readData() {
        return this.base.readData();
    }

    @Override
    protected T getDefaultData() {
        return this.base.getDefaultData();
    }

    @Override
    protected boolean writeData(T data) {
        return this.base.writeData(data);
    }
}
