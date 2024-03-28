/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for creating virtual file system.
 */
public class VirtualFileSystemBuilder {
    private static final String ROOT_FOLDER = "src/main/html/";
    /**
     * Entry point.
     * @param args Arguments (ignored)
     */
    public static void main(final String[] args) {
        final Map<String, byte[]> files = new TreeMap<>();
        readDir(new File(ROOT_FOLDER), "/", files);
        return;
    }

    /**
     * Reads all files in the directory as byte arrays.
     * @param dir Directory
     * @param prefix File prefix
     * @param result Where to put the result
     */
    private static void readDir(final File dir, final String prefix, final Map<String, byte[]> result) {
        assert dir.isDirectory();
        final File[] list = dir.listFiles();
        if (list == null)  {
            return;
        }
        for (File file : list) {
            if (file.isDirectory()) {
                readDir(file, prefix + file.getName() + "/", result);
            } else {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    result.put(prefix + file.getName(), data);
                } catch (IOException ignored) {
                    System.err.println("Can't read " + prefix + file.getName());
                }
            }
        }
    }
}
