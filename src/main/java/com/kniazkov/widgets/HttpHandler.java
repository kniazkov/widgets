/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseJson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * HTTP handler that routes incoming requests to appropriate action handlers
 * or serves static resources (HTML, JS, CSS, images).
 */
final class HttpHandler implements Handler {
    /**
     * Registered action handlers (e.g. "new instance", "synchronize", etc.).
     */
    private final Map<String, ActionHandler> actionHandlers;

    /**
     * Constructs an HTTP handler that binds application-specific logic to supported actions.
     *
     * @param application The web application
     * @param options Configuration options (e.g. logger)
     */
    HttpHandler(final Application application, final Options options) {
        this.actionHandlers = new TreeMap<>();
        this.actionHandlers.put("new instance", new NewInstance(application, options.logger));
        this.actionHandlers.put("synchronize", new Synchronize(application, options.logger));
        this.actionHandlers.put("kill", new Kill(application, options.logger));
        this.actionHandlers.put("process event", new ProcessEvent(application, options.logger));
    }

    @Override
    public Response handle(Request request) {
        // Handle action requests: /?action=...
        if (request.address.startsWith("/?")) {
            String action = request.formData.get("action");
            ActionHandler handler = actionHandlers.get(action);
            if (handler != null) {
                return new ResponseJson(handler.process(request.formData));
            }
            return null;
        }

        // Normalize root path
        String address = request.address.equals("/") ? "/index.html" : request.address;

        // Attempt to load static resource
        URL url = getClass().getResource(address);
        if (url != null) {
            try {
                byte[] rawData = Files.readAllBytes(Paths.get(url.toURI()));
                String contentType = getContentTypeByExtension(address);
                byte[] data = contentType.startsWith("text") ? unpack(rawData) : rawData;

                return new Response() {
                    @Override
                    public String getContentType() {
                        return contentType;
                    }

                    @Override
                    public byte[] getData() {
                        return data;
                    }
                };
            } catch (IOException | URISyntaxException ignored) {
                // Log failure to load resource if needed
            }
        }

        // Resource not found
        return null;
    }

    /**
     * Infers HTTP content type based on file extension.
     *
     * @param path Resource file path
     * @return MIME Type string
     */
    private static String getContentTypeByExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index < 0) return "application/unknown";

        String ext = path.substring(index + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "txt":  return "text/plain";
            case "htm":
            case "html": return "text/html";
            case "css":  return "text/css";
            case "js":   return "text/javascript";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":  return "image/" + ext;
            default:     return "application/" + ext;
        }
    }

    /**
     * Decompresses RLE-encoded data (used for text resource compression).
     *
     * @param data RLE-compressed byte array
     * @return Decompressed byte array
     */
    private static byte[] unpack(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int count = 1;

        for (byte b : data) {
            if (b < 0) {
                count = -b;
            } else {
                for (int i = 0; i < count; i++) {
                    out.write(b);
                }
                count = 1;
            }
        }

        return out.toByteArray();
    }
}
