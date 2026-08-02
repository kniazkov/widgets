/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.webserver.FileDescriptor;
import com.kniazkov.webserver.Request;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.controller.UploadEvent;
import java.util.Map;

/** Parses and validates the multipart request that carries one binary upload chunk. */
final class UploadChunk {
    /** Wire action used by the browser client. */
    static final String ACTION = "upload chunk";

    /** Application that owns client and widget state. */
    private final Application application;

    /**
     * Creates an upload action handler.
     *
     * @param application application that owns the receiving widget
     */
    UploadChunk(final Application application) {
        this.application = application;
    }

    /**
     * Validates untrusted multipart fields and delivers exactly one chunk.
     *
     * @param request parsed HTTP request
     * @return JSON acknowledgement; malformed and rejected chunks return {@code false}
     */
    JsonElement process(final Request request) {
        final JsonObject response = new JsonObject();
        response.addBoolean("result", false);
        if (request.files.size() != 1 || !request.files.containsKey("file")) {
            return response;
        }

        final Map<String, String> data = request.formData;
        final String clientValue = data.get("client");
        final String widgetValue = data.get("widget");
        final String name = data.get("name");
        final String type = data.get("type");
        if (clientValue == null || widgetValue == null || name == null || type == null) {
            return response;
        }

        final Integer fileId = parseInteger(data.get("fileId"));
        final Integer size = parseInteger(data.get("size"));
        final Integer chunkIndex = parseInteger(data.get("chunkIndex"));
        final Integer totalChunks = parseInteger(data.get("totalChunks"));
        if (fileId == null || size == null || chunkIndex == null || totalChunks == null) {
            return response;
        }

        final FileDescriptor file = request.files.get("file");
        final UploadEvent event = new UploadEvent(
            fileId,
            name,
            type,
            size,
            file == null ? null : file.data,
            chunkIndex,
            totalChunks
        );
        if (!event.isValid()) {
            return response;
        }

        final boolean accepted = this.application.uploadChunk(
            RMId.parse(clientValue),
            RMId.parse(widgetValue),
            event
        );
        response.addBoolean("result", accepted);
        return response;
    }

    /** Parses a decimal integer without allowing malformed input to escape the HTTP boundary. */
    private static Integer parseInteger(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }
}
