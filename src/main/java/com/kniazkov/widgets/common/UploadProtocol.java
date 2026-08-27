/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;

/**
 * Shared constants and immutable-by-convention values for the file-upload protocol.
 */
public final class UploadProtocol {
    /**
     * Binary chunk size selected by the server and injected into the browser bootstrap.
     */
    public static final int CHUNK_SIZE = 4 * 1024;

    /**
     * Maximum complete file size supported by the in-memory public API.
     */
    public static final int MAX_FILE_SIZE = 128 * 1024 * 1024;

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
