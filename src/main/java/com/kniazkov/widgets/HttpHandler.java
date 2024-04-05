package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import java.io.ByteArrayOutputStream;

/**
 * Handler of HTTP requests from clients.
 */
final class HttpHandler implements Handler {
    @Override
    public Response handle(Request request) {
        final String address = request.address.equals("/") ? "/index.html" : request.address;
        final byte[] data = VirtualFileSystem.get(address);
        if (data != null) {
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
        if (extension.length() > 0) {
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
