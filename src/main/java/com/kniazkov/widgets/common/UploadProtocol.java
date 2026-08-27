/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;

/**
 * Shared immutable-by-convention values for the file-upload protocol.
 */
public final class UploadProtocol {
    /**
     * Standard negative acknowledgement. Framework code must never mutate this object.
     */
    private static final JsonObject REJECTED = createRejected();

    /**
     * Returns the shared negative upload acknowledgement.
     *
     * @return singleton containing {@code {"result": false}}
     */
    public static JsonObject rejected() {
        return REJECTED;
    }

    /**
     * Creates the singleton during class initialization.
     */
    private static JsonObject createRejected() {
        final JsonObject response = new JsonObject();
        response.addBoolean("result", false);
        return response;
    }

    /**
     * Prevents construction of the protocol utility class.
     */
    private UploadProtocol() {
        /*
         * Utility class
         */
    }
}
