/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import java.util.Base64;
import java.util.Objects;

/**
 * Одноразовая передача файла внутри протокола текущей страницы.
 */
public final class DownloadFile extends Update {
    /**
     * Имя файла без пути.
     */
    private final String name;
    /**
     * MIME-тип содержимого.
     */
    private final String contentType;
    /**
     * Неизменяемый снимок файла в Base64.
     */
    private final String content;

    /**
     * Проверяет параметры и копирует содержимое в неизменяемое представление.
     *
     * @param name имя файла из латинских букв, цифр, точек, дефисов и подчёркиваний
     * @param contentType MIME-тип без параметров
     * @param content файл размером от 1 байта до 5 МиБ
     */
    public DownloadFile(final String name, final String contentType, final byte[] content) {
        this(name, contentType, encode(content));
    }

    /**
     * Создаёт команду из уже неизменяемого содержимого.
     *
     * @param name имя файла
     * @param contentType MIME-тип
     * @param content содержимое в Base64
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
     * Ограничивает расход памяти и фиксирует снимок массива.
     *
     * @param bytes содержимое файла
     * @return содержимое в Base64
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
