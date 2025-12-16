/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Locale;

/**
 * ...
 */
public class Utils {
    /**
     * Infers HTTP content type based on file extension.
     *
     * @param path Resource file path
     * @return MIME Type string
     */
    public static String getContentTypeByExtension(final String path) {
        int index = path.lastIndexOf('.');
        if (index < 0) return "application/unknown";

        String ext = path.substring(index + 1).toLowerCase(Locale.ROOT);
        switch (ext) {
            case "txt":  return "text/plain";
            case "htm":
            case "html": return "text/html";
            case "css":  return "text/css";
            case "js":   return "text/javascript";
            case "heic":
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":  return "image/" + ext;
            default:     return "application/" + ext;
        }
    }
}
