/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.client;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.Widget;
import java.util.Objects;

/**
 * Selects a UTF-16 range in a live text editor.
 */
public final class SelectText implements OnClient {
    /**
     * Target identity, without retaining the widget tree.
     */
    private final String target;
    /**
     * Inclusive start and exclusive end in UTF-16 code units.
     */
    private final int start;
    /**
     * Exclusive range end.
     */
    private final int end;

    /**
     * Creates a local UI action.
     * @param target widget in the same page runtime
     * @param start inclusive UTF-16 offset
     * @param end exclusive UTF-16 offset
     */
    public SelectText(final Widget<?> target, final int start, final int end) {
        this.target = Objects.requireNonNull(target, "target").getId().toString();
        if (start < 0 || end < start || end > 65536) {
            throw new IllegalArgumentException("Invalid selection range");
        }
        this.start = start;
        this.end = end;
    }

    @Override
    public String getAction() {
        return "select text";
    }

    @Override
    public void fillJsonObject(final JsonObject object) {
        object.addString("target", this.target);
        object.addNumber("start", this.start);
        object.addNumber("end", this.end);
    }
}
