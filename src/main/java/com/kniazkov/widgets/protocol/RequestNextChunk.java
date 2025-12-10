/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to send the next chunk of a file upload.
 */
public final class RequestNextChunk extends Update {
    /**
     * Creates a new request for the next chunk of a file upload.
     *
     * @param widget the widget identifier associated with the upload operation
     */
    public RequestNextChunk(final UId widget) {
        super(widget);
    }

    @Override
    public Update clone() {
        return new RequestNextChunk(this.getWidgetId());
    }

    @Override
    protected String getAction() {
        return "next chunk";
    }
}
