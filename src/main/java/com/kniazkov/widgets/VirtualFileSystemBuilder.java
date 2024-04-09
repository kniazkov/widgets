/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for creating virtual file system.
 */
public class VirtualFileSystemBuilder {
    /**
     * Root folder for the virtual file system from which all files are read.
     */
    private static final String ROOT_FOLDER = "src/main/html/";

    /**
     * Generated class name.
     */
    private static final String CLASS_NAME = "VirtualFileSystem";

    /**
     * Path to the file that will contain the generated class
     */
    private static final String CLASS_FILE =
        "src/main/java/com/kniazkov/widgets/"
        + CLASS_NAME
        + ".java";

    /**
     * Entry point.
     * @param args Arguments (ignored)
     */
    public static void main(final String[] args) {
        final Map<String, byte[]> files = new TreeMap<>();
        readDir(new File(ROOT_FOLDER), "/", files);
        generateClass(files);
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

    /**
     * Generates a class containing a virtual file system.
     * @param files The files on this file system
     */
    private static void generateClass(final Map<String, byte[]> files) {
        try {
            FileWriter writer = new FileWriter(CLASS_FILE);
            writer.write(
                "/*\n" +
                " * Copyright (c) 2024 Ivan Kniazkov\n" +
                " */\n");
            writer.write("package com.kniazkov.widgets;\n\n");
            writer.write(
                "import java.util.Map;\n" +
                "import java.util.TreeMap;\n\n");
            writer.write(
                "/**\n" +
                " * A virtual file system that contains the code that runs on the client browser\n" +
                " * as well as some data. This file is generated, no need to edit it.\n" +
                " */\n");
            writer.write("final class " + CLASS_NAME + " {\n");

            int index = 0;
            final Map<String, String> mapping = new TreeMap<>();
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                final String path = file.getKey();
                final String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                final boolean isTextFile;
                switch (ext) {
                    case "txt":
                    case "htm":
                    case "html":
                    case "css":
                    case "js":
                        isTextFile = true;
                        break;
                    default:
                        isTextFile = false;
                }
                final byte[] data;
                if (isTextFile) {
                    data = pack(file.getValue());
                } else {
                    data = file.getValue();
                }
                final String name = "data" + index;
                mapping.put(path, name);
                writer.write("    private static final byte[] " + name + " = {\n");
                String line = "        ";
                for (byte b : data) {
                    line = line + String.valueOf(b) + ", ";
                    if (line.length() > 90) {
                        writer.write(line + "\n");
                        line = "        ";
                    }
                }
                writer.write(line.substring(0, line.length() - 2) + "\n");
                writer.write("    };\n");
                index++;
            }

            writer.write("    private static final Map<String, byte[]> files = new TreeMap<String, byte[]>() {{\n");
            for (Map.Entry<String, String> pair : mapping.entrySet()) {
                writer.write("        put(\"" + pair.getKey() + "\", " + pair.getValue() + ");\n");
            }
            writer.write("    }};\n\n");

            writer.write(
            "    /**\n" +
                "     * Returns the contents of a file at its path.\n" +
                "     * @param path File path\n" +
                "     */\n" +
                "    public static byte[] get(final String path) {\n" +
                "        return files.get(path);\n" +
                "    }\n"
            );

            writer.write("}\n");
            writer.close();
            System.out.println("Done.");
        } catch (IOException ignored) {
            System.err.println("Can't write " + CLASS_FILE);
        }
    }

    /**
     * Data packing by RLE compression algorithm.
     * @param data Not packed data
     * @return Packed data
     */
    private static byte[] pack(final byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte p = -1;
        int count = 0;
        for (byte b : data) {
            if (b == 13) {
                continue;
            }
            if (b < 0) {
                b = '?';
            }
            if (b != p) {
                if (count < 3) {
                    for (int i = 0; i < count; i++) {
                        stream.write(p);
                    }
                } else {
                    do {
                        stream.write(count > 127 ? -127 : -count);
                        stream.write(p);
                        count -= 127;
                    } while(count > 127);
                }
                p = b;
                count = 1;
            } else {
                count++;
            }
        }
        if (count < 3) {
            for (int i = 0; i < count; i++) {
                stream.write(p);
            }
        } else {
            do {
                stream.write(count > 127 ? -127 : -count);
                stream.write(p);
                count -= 127;
            } while(count > 127);
        }
        return stream.toByteArray();
    }
}
