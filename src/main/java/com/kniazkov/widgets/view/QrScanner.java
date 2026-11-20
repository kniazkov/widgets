/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.HandlesTextEvents;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Camera with local QR decoding; applications validate and acknowledge each result.
 */
public final class QrScanner extends Panel implements HasText, HandlesTextEvents {
    /**
     * Maximum event length, including the sequence header and a full QR payload.
     */
    public static final int MAX_EVENT_LENGTH = 8192;

    /**
     * Creates a general-purpose scanner that accepts arbitrary QR text.
     * Results are data: applications must not execute or automatically open them.
     * @param messages camera messages
     * @param onCode callback that acknowledges each result with setPaused
     */
    public QrScanner(final Map<String, String> messages, final Consumer<String> onCode) {
        this("", messages, onCode);
    }

    /**
     * Optional URL prefix before a UUID; empty accepts arbitrary QR text.
     */
    private final String prefix;

    /**
     * Localized camera messages.
     */
    private final Map<String, String> messages;

    /**
     * Sequence number of the last accepted event.
     */
    private long sequence;

    /**
     * Creates a scanner with ordered events and an optional prefix-plus-UUID filter.
     * @param prefix allowed URL prefix, or empty for arbitrary QR text
     * @param messages localized messages
     * @param onCode callback that validates permissions and acknowledges the result
     */
    public QrScanner(final String prefix, final Map<String, String> messages,
                     final Consumer<String> onCode) {
        this.prefix = prefix;
        this.messages = Map.copyOf(messages);
        setWidth("100%");
        onTextInput(value -> {
            final int separator = value.indexOf('\n');
            if (separator < 1 || value.length() > MAX_EVENT_LENGTH || getRootWidget().isEmpty()) {
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
     * Acknowledges the current event and optionally pauses for a user decision.
     * @param paused whether recognition should pause
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
