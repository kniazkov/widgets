/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonArray;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP handler that routes incoming requests to appropriate action handlers
 * or serves static resources (HTML, JS, CSS, images).
 */
final class HttpHandler implements com.kniazkov.webserver.Handler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(HttpHandler.class.getName());

    /**
     * Entity tags may contain commas; splitting a header on commas is not sufficient.
     */
    private static final Pattern ENTITY_TAG = Pattern.compile(
        "(?:W/)?\"[\\x21\\x23-\\x7e\\x80-\\xff]*\"");

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

    /**
     * Wraps the two classic protocol scripts in one lexical scope per history entry.
     * No dynamic evaluation or browser-global widget registry is required.
     *
     * @return page runtime factory source
     * @throws IOException if a bundled script cannot be read
     */
    private String pageRuntime() throws IOException {
        final StringBuilder source = new StringBuilder("function createPageRuntime(page) {\n");
        for (final String name : List.of("widgets", "client")) {
            try (InputStream input = getClass().getResourceAsStream("/scripts/" + name + ".js")) {
                if (input == null) {
                    throw new IOException("Missing page runtime script: " + name);
                }
                source.append(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                source.append('\n');
            }
        }
        source.append("return { initClient, mainCycle, disposeClient, showClientError };\n}\n");
        final String code = source.toString();
        return this.options.isDebug() ? code : code.replaceAll(
            "\\blog\\([^;]*\\)\\s*;", "/* $0 */"
        );
    }

    @Override
    public Response handle(final Request request, final Environment environment)
            throws ServerException {
        try {
            return this.route(request, environment);
        } catch (final ServerException | RuntimeException | Error failure) {
            LOGGER.log(Level.SEVERE, "Widgets HTTP request failed; "
                + RequestDiagnostics.describe(request), failure);
            throw failure;
        }
    }

    /**
     * Routes a request inside the shared logging boundary.
     *
     * @param request HTTP request
     * @param environment response environment
     * @return response
     * @throws ServerException if request processing fails
     */
    private Response route(final Request request, final Environment environment)
            throws ServerException {
        final ResponseFactory responses = environment.getResponseFactory();
        final HttpMethod method = request.getHeaders().getMethod();
        final String requestPath = request.getPath().getPath();
        final Map<String, String> parameters = flatten(
            method == HttpMethod.POST ? request.getForm() : request.getQuery()
        );

        /*
         * Handle action requests: /?action=...
         */
        final boolean rootAction = method == HttpMethod.GET
            && "/".equals(requestPath)
            && parameters.containsKey("action");
        if (method == HttpMethod.POST || rootAction) {
            final String action = parameters.get("action");
            if (action == null) {
                final String requestId = UUID.randomUUID().toString();
                LOGGER.warning("Widgets action request rejected: missing action; requestId="
                    + requestId + " " + RequestDiagnostics.describe(request));
                return responses.custom(HttpStatus.NOT_FOUND, ContentType.TEXT_PLAIN, new byte[0])
                    .setHeader("X-Widgets-Request-Id", requestId)
                    .setHeader("Cache-Control", "no-store").build();
            }
            if (method == HttpMethod.POST && "upload chunk".equals(action)) {
                return responses.fromJson(
                    this.handleUploadChunk(request, parameters).toString()
                ).build();
            }
            if (method == HttpMethod.POST && "report error".equals(action)) {
                this.application.reportBrowserError(parameters);
                return responses.fromJson("{}").build();
            }
            final ActionHandler handler = actionHandlers.get(action);
            if (handler != null) {
                return responses.fromJson(
                    handler.process(parameters).toString()
                ).build();
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

        final int dot = address.lastIndexOf('.');
        final ContentType contentType = ContentType.fromExtension(
            dot > address.lastIndexOf('/') ? address.substring(dot + 1) : "");
        final boolean removeLogs = contentType == ContentType.TEXT_JAVASCRIPT && !options.isDebug();

        try {
            final URL url = isBundledWebResource(address)
                ? getClass().getResource(address)
                : null;
            final byte[] data;

            if ("/scripts/page-runtime.js".equals(address)) {
                data = this.pageRuntime().getBytes(StandardCharsets.UTF_8);
            } else if (url != null) {
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
                            final JsonArray addresses = new JsonArray();
                            for (final String path : this.application.getPageAddresses()) {
                                addresses.addString(path);
                            }
                            final JsonObject obj = new JsonObject();
                            for (final Map.Entry<String, String> entry : parameters.entrySet()) {
                                obj.addString(entry.getKey(), entry.getValue());
                            }
                            code = code
                                .replace("{sessionId}", UUID.randomUUID().toString())
                                .replace("{address}", requestPath)
                                .replace("{data}", escapeInlineScriptData(obj.toString()))
                                .replace("__PAGE_ADDRESSES__",
                                    escapeInlineScriptData(addresses.toString()))
                                .replace(
                                    "__UPLOAD_CHUNK_SIZE__",
                                    Integer.toString(this.options.getChunkSize())
                                )
                                .replace(
                                    "__MAX_UPLOAD_FILE_SIZE__",
                                    Integer.toString(this.options.getMaxFileSize())
                                )
                                .replace(
                                    "__WEB_FONT_STYLESHEETS__",
                                    webFontStylesheets(this.options)
                                );
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
                StaticSource selected = null;
                for (final StaticSource source : this.options.getStaticSources()) {
                    if (source.matches(requestPath) && (selected == null
                            || source.getPrefix().length() > selected.getPrefix().length())) {
                        selected = source;
                    }
                }
                data = selected == null
                    ? StaticSource.readDirectory(Paths.get(this.options.getWwwRoot()),
                        requestPath.substring(1))
                    : selected.read(requestPath);
            }

            if (replaceAddress) {
                /*
                 * Page bootstrap contains a new session and request-specific parameters.
                 */
                return responses.custom(HttpStatus.OK, contentType, data)
                    .setHeader("Cache-Control", "no-store").build();
            }
            final String hash = StaticSource.contentHash(data);
            final List<String> versions = request.getQuery().get("widgets-version");
            if (versions != null && (versions.size() != 1 || !hash.equals(versions.get(0)))) {
                /*
                 * Never publish changed bytes under an old immutable URL.
                 */
                return responses.custom(HttpStatus.NOT_FOUND, ContentType.TEXT_PLAIN, new byte[0])
                    .setHeader("Cache-Control", "no-store").build();
            }
            final String tag = "\"" + hash + "\"";
            final boolean unchanged = matchesEntityTag(request, tag);
            return responses.custom(unchanged ? HttpStatus.NOT_MODIFIED : HttpStatus.OK,
                contentType, unchanged ? new byte[0] : data)
                .setHeader("ETag", tag)
                .setHeader("Cache-Control", versions == null ? "private, no-cache"
                    : "private, max-age=31536000, immutable")
                .setHeader("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    ZonedDateTime.now(ZoneOffset.UTC)))
                .build();

        } catch (final SecurityException error) {
            return responses.forbidden();
        } catch (final IOException error) {
            LOGGER.log(
                Level.SEVERE,
                "File not found or cannot be read: '" + requestPath + "'",
                error
            );
        }

        /*
         * Resource not found
         */
        return responses.notFound();
    }

    /**
     * Performs weak If-None-Match comparison for an existing, readable GET resource.
     * Invalid lists are ignored rather than accidentally matching a tag inside them.
     * @param request request carrying zero or more conditional headers
     * @param tag current strong entity tag
     * @return whether the stored response can be reused
     */
    private static boolean matchesEntityTag(final Request request, final String tag) {
        final List<String> values = new java.util.ArrayList<>();
        request.getHeaders().getValues().forEach((name, items) -> {
            if ("If-None-Match".equalsIgnoreCase(name)) {
                values.addAll(items);
            }
        });
        final String value = String.join(",", values).trim();
        if ("*".equals(value)) {
            return true;
        }
        final Matcher matcher = ENTITY_TAG.matcher(value);
        int end = 0;
        boolean matched = false;
        while (matcher.find()) {
            final String separator = value.substring(end, matcher.start());
            if (!separator.matches("[ \\t,]*") || (end > 0 && !separator.contains(","))) {
                return false;
            }
            final String candidate = matcher.group();
            matched |= tag.equals(candidate.startsWith("W/") ? candidate.substring(2) : candidate);
            end = matcher.end();
        }
        return matched && value.substring(end).matches("[ \\t,]*");
    }

    /**
     * Validates and delivers one multipart binary upload chunk.
     *
     * @param request immutable web-server request
     * @param parameters flattened multipart fields
     * @return serialized upload acknowledgement
     * @throws ServerException if the temporary upload data cannot be read
     */
    private JsonObject handleUploadChunk(
            final Request request,
            final Map<String, String> parameters) throws ServerException {
        final List<UploadedFile> files = request.getFiles().get("chunk");
        if (request.getFiles().size() != 1 || files == null || files.size() != 1) {
            return UploadProtocol.rejected();
        }
        final UploadedFile chunk = files.get(0);
        if (chunk.getSize() > this.options.getChunkSize()) {
            return UploadProtocol.rejected();
        }
        final String client = parameters.get("client");
        final String widget = parameters.get("widget");
        if (client == null || widget == null) {
            return UploadProtocol.rejected();
        }
        final RMId clientId = RMId.parse(client);
        final RMId widgetId = RMId.parse(widget);
        if (!clientId.isValid() || !widgetId.isValid()) {
            return UploadProtocol.rejected();
        }
        try {
            final int fileId = Integer.parseInt(parameters.get("fileId"));
            final int chunkIndex = Integer.parseInt(parameters.get("chunkIndex"));
            return this.application.uploadChunk(
                clientId,
                widgetId,
                fileId,
                chunkIndex,
                chunk.readAllBytes(),
                parameters.get("lastUpdate")
            );
        } catch (final NumberFormatException | NullPointerException ignored) {
            return UploadProtocol.rejected();
        }
    }

    /**
     * Preserves the 1.x last-value-wins form contract for repeated values in the 2.0 API.
     */
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
     * Builds safe stylesheet links for the fonts registered in application options.
     * Duplicate URIs are emitted only once.
     *
     * @param options application options
     * @return HTML fragment for the document head
     */
    private static String webFontStylesheets(final Options options) {
        final Set<String> stylesheets = new LinkedHashSet<>();
        options.getWebFonts().forEach(font -> stylesheets.add(
            font.getStylesheetUri().toASCIIString()
        ));
        final StringBuilder result = new StringBuilder();
        for (final String stylesheet : stylesheets) {
            if (!result.isEmpty()) {
                result.append('\n').append("        ");
            }
            result.append("<link rel=\"stylesheet\" href=\"")
                .append(escapeHtmlAttribute(stylesheet))
                .append("\">");
        }
        return result.toString();
    }

    /**
     * Escapes a value before inserting it into a quoted HTML attribute.
     *
     * @param value source attribute value
     * @return escaped attribute value
     */
    private static String escapeHtmlAttribute(final String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
