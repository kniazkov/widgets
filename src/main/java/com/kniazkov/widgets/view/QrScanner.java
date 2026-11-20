package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.HandlesTextEvents;
import java.util.Map;

/** Камера с локальным декодированием QR; приложение проверяет код и подтверждает событие. */
public final class QrScanner extends Panel implements HasText, HandlesTextEvents {
    private final String prefix;
    private final Map<String, String> messages;
    private long sequence;

    /** Создаёт сканер с разрешённым префиксом, локализованными сообщениями и обработчиком кода. */
    public QrScanner(String prefix, Map<String, String> messages, java.util.function.Consumer<String> onCode) {
        this.prefix = prefix;
        this.messages = Map.copyOf(messages);
        setWidth("100%");
        onTextInput(value -> {
            final int separator = value.indexOf('\n');
            if (separator < 1 || value.length() > 2048 || getRootWidget().isEmpty()) return;
            final long received;
            try { received = Long.parseLong(value.substring(0, separator)); }
            catch (NumberFormatException invalid) { return; }
            if (received != sequence + 1) return;
            sequence = received;
            onCode.accept(value.substring(separator + 1));
        });
        setPaused(false);
    }

    @Override public String getType() { return "qr scanner"; }

    /** Подтверждает последнее событие; пауза сохраняется до закрытия подтверждения приложения. */
    public void setPaused(boolean paused) {
        final var data = new JsonObject();
        data.addString("prefix", prefix); data.addNumber("ack", sequence); data.addBoolean("paused", paused);
        final var texts = data.createObject("messages"); messages.forEach(texts::addString);
        setText(data.toString());
    }
}
