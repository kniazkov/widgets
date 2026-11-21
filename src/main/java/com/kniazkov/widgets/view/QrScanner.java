/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.HandlesTextEvents;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Video-only camera with local QR decoding and application-owned controls.
 * Starts disabled: call {@link #enable()} to request camera access and {@link #disable()}
 * to release it. Recognition pauses keep the camera open. Applications render statuses
 * through {@link #onStatus(Consumer)} and acknowledge codes with {@link #setPaused(boolean)}.
 */
public final class QrScanner extends Panel implements HasText, HandlesTextEvents, HasDisabledState {
    /**
     * Maximum event length, including the sequence header and a full QR payload.
     */
    public static final int MAX_EVENT_LENGTH = 8192;

    /**
     * Camera states reported by the browser, without presentation or localized text.
     */
    public enum Status {
        /**
         * Waiting for camera access.
         */
        OPENING,
        /**
         * Camera is ready to recognize codes.
         */
        READY,
        /**
         * Recognition awaits acknowledgement or a user decision.
         */
        WAITING,
        /**
         * Camera access needs HTTPS or localhost.
         */
        HTTPS_REQUIRED,
        /**
         * The user denied camera access.
         */
        DENIED,
        /**
         * Camera, decoder or video playback is unavailable.
         */
        UNAVAILABLE,
        /**
         * A decoded code does not match the configured filter.
         */
        INVALID,
        /**
         * Camera stopped because the page or scanner became hidden.
         */
        CLOSED
    }

    /**
     * Optional URL prefix before a UUID; empty accepts arbitrary QR text.
     */
    private final String prefix;

    /**
     * Application-owned status presentation.
     */
    private Consumer<Status> statusHandler = status -> { };

    /**
     * Sequence number of the last accepted code event.
     */
    private long sequence;

    /**
     * Creates a disabled scanner that accepts arbitrary QR text.
     * Results are data: applications must not execute or automatically open them.
     * @param onCode callback that acknowledges each result with setPaused
     */
    public QrScanner(final Consumer<String> onCode) {
        this("", onCode);
    }

    /**
     * Creates a disabled scanner with ordered events and an optional prefix-plus-UUID filter.
     * @param prefix allowed URL prefix, or empty for arbitrary QR text
     * @param onCode callback that validates permissions and acknowledges the result
     */
    public QrScanner(final String prefix, final Consumer<String> onCode) {
        this.prefix = Objects.requireNonNull(prefix);
        Objects.requireNonNull(onCode);
        setWidth("100%");
        disable();
        onTextInput(value -> {
            if (!isInteractionEnabled() || getRootWidget().isEmpty()
                    || value.length() > MAX_EVENT_LENGTH) {
                return;
            }
            if (value.startsWith("status\n")) {
                final Status status;
                try {
                    status = Status.valueOf(value.substring(7));
                } catch (IllegalArgumentException invalid) {
                    return;
                }
                statusHandler.accept(status);
                return;
            }
            final int separator = value.indexOf('\n');
            if (separator < 1) {
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

    /**
     * Compatibility overload; messages are no longer rendered inside the scanner.
     * Use onStatus to display localized messages in application-owned widgets.
     * @param messages previous message map, retained only for source compatibility
     * @param onCode callback that acknowledges each result with setPaused
     */
    public QrScanner(final Map<String, String> messages, final Consumer<String> onCode) {
        this("", messages, onCode);
    }

    /**
     * Compatibility overload; the scanner starts disabled and renders only video.
     * @param prefix allowed URL prefix, or empty for arbitrary QR text
     * @param messages previous message map, retained only for source compatibility
     * @param onCode callback that acknowledges each result with setPaused
     */
    public QrScanner(final String prefix, final Map<String, String> messages,
                     final Consumer<String> onCode) {
        this(prefix, onCode);
        Objects.requireNonNull(messages);
    }

    @Override
    public String getType() {
        return "qr scanner";
    }

    /**
     * Replaces the camera status callback. Statuses do not consume code sequence numbers.
     * Notifications are informational; they do not grant application permissions.
     * @param handler callback for browser-reported camera status
     */
    public void onStatus(final Consumer<Status> handler) {
        this.statusHandler = Objects.requireNonNull(handler);
    }

    /**
     * Acknowledges the current event and optionally pauses recognition, keeping the camera open.
     * This does not enable a disabled scanner. Use disable to release the camera.
     * @param paused whether recognition should pause
     */
    public void setPaused(final boolean paused) {
        final var data = new JsonObject();
        data.addString("prefix", prefix);
        data.addNumber("ack", sequence);
        data.addBoolean("paused", paused);
        setText(data.toString());
    }
}
