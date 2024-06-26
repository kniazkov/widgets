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
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handler of HTTP requests from clients.
 */
final class HttpHandler implements Handler {
    /**
     * Handlers for actions that are requested by clients.
     */
    private final Map<String, ActionHandler> actionHandlers;

    /**
     * Constructor.
     * @param application Web application to be run
     * @param options Various options
     */
    HttpHandler(final @NotNull Application application, final @NotNull Options options) {
        this.actionHandlers = new TreeMap<>();
        this.actionHandlers.put("new instance", new NewInstance(application, options.logger));
        this.actionHandlers.put("synchronize", new Synchronize(application, options.logger));
        this.actionHandlers.put("kill", new Kill(application, options.logger));
        this.actionHandlers.put("process event", new ProcessEvent(application, options.logger));
    }

    @Override
    public Response handle(Request request) {
        final String address;
        if (request.address.startsWith("/?")) {
            final ActionHandler actionHandler = this.actionHandlers.get(request.formData.get("action"));
            if (actionHandler != null) {
                return new ResponseJson(actionHandler.process(request.formData));
            }
            return null;
        } else if (request.address.equals("/")) {
            address = "/index.html";
        } else {
            address = request.address;
        }
        final URL url = getClass().getResource(address);
        if (url != null) {
            try {
                final byte[] data = Files.readAllBytes(Paths.get(url.toURI()));
                final String type = HttpHandler.getContentTypeByExtension(address);
                return new Response() {
                    @Override
                    public String getContentType() {
                        return type;
                    }

                    @Override
                    public byte[] getData() {
                        return type.startsWith("text") ? HttpHandler.unpack(data) : data;
                    }
                };
            } catch (IOException | URISyntaxException ignored) {
            }
        }
        return null;
    }

    /**
     * Returns HTTP content type by file extension.
     * @param path File path
     * @return HTTP content type
     */
    private static String getContentTypeByExtension(final String path) {
        String extension = "";
        int index = path.lastIndexOf('.');
        if (index > 0)
            extension = path.substring(index + 1).toLowerCase();
        String type = "application/unknown";
        if (!extension.isEmpty()) {
            switch(extension)
            {
                case "txt":
                    type = "text/plain";
                    break;
                case "htm":
                case "html":
                    type = "text/html";
                    break;
                case "css":
                    type = "text/css";
                    break;
                case "js":
                    type = "text/javascript";
                    break;
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                    type = "image/" + extension;
                    break;
                default:
                    type = "application/" + extension;
            }
        }
        return type;
    }

    /**
     * Decompresses data packed using RLE compression.
     * @param data Compressed data
     * @return Unpacked data
     */
    private static byte[] unpack(final byte[] data) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int count = 1;
        for (final byte b : data) {
            if (b < 0) {
                count = -b;
                continue;
            }
            for (int index = 0; index < count; index++) {
                stream.write(b);
            }
            count = 1;
        }
        return stream.toByteArray();
    }
}
