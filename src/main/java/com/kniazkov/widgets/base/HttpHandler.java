/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Utils;
import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.ServerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
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
    public Response handle(final Request request, final Environment environment)
            throws ServerException {
        final ResponseFactory responses = environment.getResponseFactory();
        final HttpMethod method = request.getHeaders().getMethod();
        final String requestPath = request.getPath().getPath();
        final Map<String, String> parameters = flatten(
            method == HttpMethod.POST ? request.getForm() : request.getQuery()
        );

        // Handle action requests: /?action=...
        final boolean rootQuery = method == HttpMethod.GET
            && "/".equals(requestPath)
            && !request.getQuery().isEmpty();
        if (method == HttpMethod.POST || rootQuery) {
            final String action = parameters.get("action");
            final ActionHandler handler = actionHandlers.get(action);
            if (handler != null) {
                return responses.fromJson(handler.process(parameters).toString()).build();
            }
            return responses.notFound();
        }

        final String address;
        final boolean replaceAddress;

        if (this.application.hasPage(requestPath)) {
            /*
                For all pages of the project, we actually load the same index.html page, replacing
                the target page address in it, which is sent to the server when a new client
                is initialized.
             */
            address = "/index.html";
            replaceAddress = true;
        } else {
            address = requestPath;
            replaceAddress = false;
        }

        final String contentType = Utils.getContentTypeByExtension(address);
        final boolean removeLogs = contentType.equals("text/javascript") && !options.isDebug();

        try {
            final URL url = isBundledWebResource(address)
                ? getClass().getResource(address)
                : null;
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
                        String code = buffer.toString(StandardCharsets.UTF_8);
                        if (replaceAddress) {
                            final JsonObject obj = new JsonObject();
                            for (final Map.Entry<String, String> entry : parameters.entrySet()) {
                                obj.addString(entry.getKey(), entry.getValue());
                            }
                            code = code
                                .replace("{sessionId}", UUID.randomUUID().toString())
                                .replace("{address}", requestPath)
                                .replace("{data}", escapeInlineScriptData(obj.toString()));
                        }
                        if (removeLogs) {
                            code = code.replaceAll(
                                "\\blog\\([^;]*\\)\\s*;",
                                "/* $0 */"
                            );
                        }
                        data = code.getBytes(StandardCharsets.UTF_8);
                    } else {
                        data = buffer.toByteArray();
                    }
                }
            } else {
                final Path root = Paths.get(this.options.getWwwRoot()).toRealPath();
                final String relative = requestPath.startsWith("/")
                    ? requestPath.substring(1)
                    : requestPath;
                final Path path = root.resolve(relative).toRealPath();
                if (!path.startsWith(root)) {
                    return responses.forbidden();
                }
                data = Files.readAllBytes(path);
            }

            return responses.custom(
                HttpStatus.OK,
                ContentType.fromString(contentType),
                data
            ).build();

        } catch (IOException e) {
            LOGGER.warning("File not found or cannot be read: '" + requestPath + "': " + e);
        }

        // Resource not found
        return responses.notFound();
    }

    /** Preserves the 1.x last-value-wins form contract for repeated values in the 2.0 API. */
    private static Map<String, String> flatten(final Map<String, List<String>> source) {
        final Map<String, String> result = new TreeMap<>();
        for (final Map.Entry<String, List<String>> entry : source.entrySet()) {
            final List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                result.put(entry.getKey(), values.get(values.size() - 1));
            }
        }
        return result;
    }
    /**
     * Escapes JSON before embedding it inside an HTML script element.
     *
     * @param json serialized JSON
     * @return equivalent JavaScript source that cannot start an HTML end tag
     */
    private static String escapeInlineScriptData(final String json) {
        return json.replace("<", "\\u003c");
    }

    /**
     * Returns whether the path identifies a bundled public web resource.
     *
     * @param address requested classpath address
     * @return true if the resource belongs to the public web bundle
     */
    private static boolean isBundledWebResource(final String address) {
        return "/index.html".equals(address)
            || "/style.css".equals(address)
            || address.startsWith("/scripts/")
            || address.startsWith("/fonts/");
    }

}
