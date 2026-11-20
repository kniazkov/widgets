/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.HandlesTextEvents;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Камера с локальным декодированием QR; приложение проверяет код и подтверждает событие.
 */
public final class QrScanner extends Panel implements HasText, HandlesTextEvents {
    /**
     * Разрешённый префикс ссылки перед UUID.
     */
    private final String prefix;

    /**
     * Локализованные сообщения камеры.
     */
    private final Map<String, String> messages;

    /**
     * Номер последнего принятого события.
     */
    private long sequence;

    /**
     * Создаёт сканер с проверкой последовательности событий.
     * @param prefix разрешённый префикс ссылки
     * @param messages локализованные сообщения
     * @param onCode обработчик кода, который проверяет права и подтверждает событие
     */
    public QrScanner(final String prefix, final Map<String, String> messages,
                     final Consumer<String> onCode) {
        this.prefix = prefix;
        this.messages = Map.copyOf(messages);
        setWidth("100%");
        onTextInput(value -> {
            final int separator = value.indexOf('\n');
            if (separator < 1 || value.length() > 2048 || getRootWidget().isEmpty()) {
                return;
            }
            final long received;
            try {
                received = Long.parseLong(value.substring(0, separator));
            } catch (NumberFormatException invalid) {
                return;
            }
            if (received != sequence + 1) {
                return;
            }
            sequence = received;
            onCode.accept(value.substring(separator + 1));
        });
        setPaused(false);
    }

    @Override
    public String getType() {
        return "qr scanner";
    }

    /**
     * Подтверждает событие и задаёт паузу до решения пользователя.
     * @param paused нужно ли приостановить распознавание
     */
    public void setPaused(final boolean paused) {
        final var data = new JsonObject();
        data.addString("prefix", prefix);
        data.addNumber("ack", sequence);
        data.addBoolean("paused", paused);
        final var texts = data.createObject("messages");
        messages.forEach(texts::addString);
        setText(data.toString());
    }
}
