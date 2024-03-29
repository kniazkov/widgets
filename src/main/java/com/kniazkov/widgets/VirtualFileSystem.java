/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Map;
import java.util.TreeMap;

/**
 * A virtual file system that contains the code that runs on the client browser
 * as well as some data. This file is generated, no need to edit it.
 */
class VirtualFileSystem {
    private static final byte[] data0 = {
        60, 33, 68, 79, 67, 84, 89, 80, 69, 32, 104, 116, 109, 108, 62, 10, 60, 104, 116, 109, 
        108, 32, 108, 97, 110, 103, 61, 34, 101, 110, 34, 62, 10, 32, 32, 32, 32, 60, 104, 
        101, 97, 100, 62, 10, 32, 32, 32, 32, 32, 32, 32, 32, 60, 109, 101, 116, 97, 32, 99, 
        104, 97, 114, 115, 101, 116, 61, 34, 85, 84, 70, 45, 56, 34, 62, 10, 32, 32, 32, 32, 
        60, 47, 104, 101, 97, 100, 62, 10, 32, 32, 32, 32, 60, 98, 111, 100, 121, 62, 10, 32, 
        32, 32, 32, 32, 32, 32, 32, 60, 112, 62, 73, 116, 32, 119, 111, 114, 107, 115, 46, 
        60, 47, 112, 62, 10, 32, 32, 32, 32, 60, 47, 98, 111, 100, 121, 62, 10, 60, 47, 104, 
        116, 109, 108, 62, 10
    };
    private static final byte[] data1 = {
        118, 97, 114, 32, 100, 111, 83, 111, 109, 101, 116, 104, 105, 110, 103, 32, 61, 32, 
        102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, 125, 59, 10
    };
    private static final Map<String, byte[]> files = new TreeMap<String, byte[]>() {{
        put("/index.html", data0);
        put("/script/lib.js", data1);
    }};

    /**
     * Returns the contents of a file at its path.
     * @param path File path
     */
    public static byte[] get(final String path) {
        return files.get(path);
    }
}
