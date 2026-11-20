/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import java.util.Base64;
import java.util.Objects;

/**
 * A one-time file transfer through the current page's protocol.
 */
public final class DownloadFile extends Update {
    /**
     * The file name without a path.
     */
    private final String name;
    /**
     * The MIME type of the content.
     */
    private final String contentType;
    /**
     * An immutable Base64-encoded snapshot of the file.
     */
    private final String content;

    /**
     * Validates the arguments and copies the content into an immutable representation.
     *
     * @param name file name using ASCII letters, digits, dots, hyphens, and underscores
     * @param contentType MIME type without parameters
     * @param content file content between 1 byte and 5 MiB
     */
    public DownloadFile(final String name, final String contentType, final byte[] content) {
        this(name, contentType, encode(content));
    }

    /**
     * Creates a command from content that is already immutable.
     *
     * @param name file name
     * @param contentType MIME type
     * @param content Base64-encoded content
     */
    private DownloadFile(final String name, final String contentType, final String content) {
        super(RMId.INVALID);
        if (!Objects.requireNonNull(name).matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
                || !Objects.requireNonNull(contentType)
                    .matches("[a-z0-9][a-z0-9.+-]*/[a-z0-9][a-z0-9.+-]*")) {
            throw new IllegalArgumentException("Некорректное имя или MIME-тип файла");
        }
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }

    /**
     * Limits memory usage and captures a snapshot of the array.
     *
     * @param bytes file content
     * @return Base64-encoded content
     */
    private static String encode(final byte[] bytes) {
        if (Objects.requireNonNull(bytes).length == 0 || bytes.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Размер файла должен быть от 1 байта до 5 МиБ");
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public Update clone() {
        return new DownloadFile(this.name, this.contentType, this.content);
    }

    @Override
    protected String getAction() {
        return "download file";
    }

    @Override
    protected void fillJsonObject(final JsonObject obj) {
        obj.addString("name", this.name);
        obj.addString("contentType", this.contentType);
        obj.addString("content", this.content);
    }
}
