/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.common.Utils;
import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.webserver.Method;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseJson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Application.
     */
    private final Application application;

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
        this.application = application;
        this.actionHandlers = new TreeMap<>();
        this.actionHandlers.put("new instance", new CreateClient(application));
        this.actionHandlers.put("synchronize", new Synchronize(application));
        this.actionHandlers.put("kill", new KillClient(application));
        this.options = options;
    }

    @Override
    public Response handle(final Request request) {
        // Handle action requests: /?action=...
        if (request.method == Method.POST || request.address.startsWith("/?")) {
            final String action = request.formData.get("action");
            final ActionHandler handler = actionHandlers.get(action);
            if (handler != null) {
                return new ResponseJson(handler.process(request.formData));
            }
            return null;
        }

        final String address;
        final boolean replaceAddress;

        if (this.application.hasPage(request.path)) {
            /*
                For all pages of the project, we actually load the same index.html page, replacing
                the target page address in it, which is sent to the server when a new client
                is initialized.
             */
            address = "/index.html";
            replaceAddress = true;
        } else {
            address = request.path;
            replaceAddress = false;
        }

        final String contentType = Utils.getContentTypeByExtension(address);
        final boolean removeLogs = contentType.equals("text/javascript") && !options.debug;

        try {
            final URL url = getClass().getResource(address);
            final byte[] data;

            if (url != null) {
                try (InputStream in = url.openStream();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] tmp = new byte[4096];
                    int count;
                    while ((count = in.read(tmp)) >= 0) {
                        buffer.write(tmp, 0, count);
                    }
                    if (replaceAddress || removeLogs) {
                        String code = buffer.toString();
                        if (replaceAddress) {
                            final JsonObject obj = new JsonObject();
                            for (final String key : request.formData.keySet()) {
                                obj.addString(key, request.formData.get(key));
                            }
                            code = code
                                .replace("{address}", request.path)
                                .replace("{data}", obj.toString());
                        }
                        if (removeLogs) {
                            code = code.replaceAll(
                                "\\blog\\([^;]*\\)\\s*;",
                                "/* $0 */"
                            );
                        }
                        data = code.getBytes();
                    } else {
                        data = buffer.toByteArray();
                    }
                }
            } else {
                Path path = Paths.get(this.options.wwwRoot, request.path);
                data = Files.readAllBytes(path);
            }

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

        } catch (IOException e) {
            LOGGER.warning("File not found or cannot be read: '" + request.address + "': " + e);
        }

        // Resource not found
        return null;
    }
}
