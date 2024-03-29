package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;

/**
 * Handler of HTTP requests from clients.
 */
final class HttpHandler implements Handler {
    @Override
    public Response handle(Request request) {
        final String address = request.address.equals("/") ? "/index.html" : request.address;
        final byte[] data = VirtualFileSystem.get(address);
        if (data != null) {
            return new Response() {
                @Override
                public String getContentType() {
                    return HttpHandler.getContentTypeByExtension(address);
                }

                @Override
                public byte[] getData() {
                    return data;
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
}
