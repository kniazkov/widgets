/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseJson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * HTTP handler that routes incoming requests to appropriate action handlers
 * or serves static resources (HTML, JS, CSS, images).
 */
final class HttpHandler implements com.kniazkov.webserver.Handler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    /**
     * Registered action handlers (e.g. "new instance", "synchronize", etc.).
     */
    private final Map<String, ActionHandler> actionHandlers;

    /**
     * Various options.
     */
    private final Options options;

    /**
     * Constructs an HTTP handler that binds application-specific logic to supported actions.
     *
     * @param application the web application
     * @param options configuration options
     */
    HttpHandler(final Application application, final Options options) {
        this.actionHandlers = new TreeMap<>();
        this.actionHandlers.put("new instance", new CreateClient(application));
        this.actionHandlers.put("synchronize", new Synchronize(application));
        this.actionHandlers.put("kill", new KillClient(application));
        this.options = options;
    }

    @Override
    public Response handle(Request request) {
        // Handle action requests: /?action=...
        if (request.address.startsWith("/?")) {
            final String action = request.formData.get("action");
            final ActionHandler handler = actionHandlers.get(action);
            if (handler != null) {
                return new ResponseJson(handler.process(request.formData));
            }
            return null;
        }

        // Normalize root path
        final String address = request.address.equals("/") ? "/index.html" : request.address;
        final Path path;

        try {
            // Attempt to load static resource
            final URL url = getClass().getResource(address);
            if (url != null) {
                path = Paths.get(url.toURI());
            } else {
                path = Paths.get(this.options.wwwRoot, request.address);
            }
            final byte[] data = Files.readAllBytes(path);
            final String contentType = getContentTypeByExtension(address);
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
            LOGGER.warning("File not found: '" + request.address + "'");
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
}
