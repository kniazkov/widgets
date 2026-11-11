/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.HasCheckedState;
import com.kniazkov.widgets.view.HasSelectedIndex;
import com.kniazkov.widgets.view.HasTextInput;
import com.kniazkov.widgets.view.ModalPopup;
import com.kniazkov.widgets.view.RadioButton;
import com.kniazkov.widgets.view.SortableSection;
import com.kniazkov.widgets.view.Widget;
import java.util.Optional;
import java.util.Set;

/**
 * Pure checks of browser event capabilities and payloads before any model mutation.
 */
final class EventValidation {
    /**
     * Utility class.
     */
    private EventValidation() {
    }

    /**
     * Returns a stable rejection code; never includes untrusted input in the response.
     */
    static Optional<String> rejection(final Event<?> event, final Widget<?> widget,
            final JsonObject data) {
        if (event == Event.STUB) {
            return Optional.of("unknownEvent");
        }
        if (!widget.isInteractionEnabled()) {
            return Optional.of("inactiveWidget");
        }
        if (!supports(event, widget)) {
            return Optional.of("unsupportedEvent");
        }
        if (data == null || !valid(event, widget, data)) {
            return Optional.of("invalidPayload");
        }
        return Optional.empty();
    }

    /**
     * Describes which widget capabilities permit browser-originated events.
     */
    private static boolean supports(final Event<?> event, final Widget<?> widget) {
        if (event == Event.TEXT_INPUT) {
            return widget instanceof HasTextInput;
        }
        if (event == Event.CHECK) {
            return widget instanceof HasCheckedState;
        }
        if (event == Event.SELECT) {
            return widget instanceof HasSelectedIndex;
        }
        if (event == Event.REORDER) {
            return widget instanceof SortableSection;
        }
        if (event == Event.UPLOAD) {
            return widget instanceof FileLoader;
        }
        if (event == Event.DISMISS) {
            return widget instanceof ModalPopup popup && popup.isCloseOnOutsideClick();
        }
        if (event == Event.FOCUS || event == Event.BLUR) {
            return widget instanceof HandlesFocusEvents;
        }
        return pointer(event) && widget instanceof HandlesPointerEvents;
    }

    /**
     * Checks complete payload shapes, numeric domains, and string limits.
     */
    private static boolean valid(final Event<?> event, final Widget<?> widget,
            final JsonObject data) {
        if (event == Event.TEXT_INPUT) {
            return fields(data, "text")
                && text(data.get("text"), ((HasTextInput) widget).getMaxInputLength());
        }
        if (event == Event.CHECK) {
            return fields(data, "state") && data.get("state").isBoolean()
                && (!(widget instanceof RadioButton) || data.get("state").booleanValue());
        }
        if (event == Event.SELECT) {
            return fields(data, "index") && integer(data.get("index"), -1, Integer.MAX_VALUE)
                && ((HasSelectedIndex) widget).acceptsSelectedIndex(data.get("index").intValue());
        }
        if (event == Event.REORDER) {
            return fields(data, "revision", "child", "before")
                && integer(data.get("revision"), 0, Integer.MAX_VALUE)
                && text(data.get("child"), 64) && !data.get("child").stringValue().isEmpty()
                && text(data.get("before"), 64);
        }
        if (event == Event.UPLOAD) {
            return upload(widget, data);
        }
        if (pointer(event)) {
            return pointerData(data);
        }
        return data.isEmpty();
    }

    /**
     * Validates upload metadata before FileLoader allocates storage or invokes a controller.
     */
    private static boolean upload(final Widget<?> widget, final JsonObject data) {
        if (!fields(data, "fileId", "name", "type", "size", "totalChunks")
                || !integer(data.get("fileId"), 1, Integer.MAX_VALUE)
                || !integer(data.get("size"), 0, Integer.MAX_VALUE)
                || !integer(data.get("totalChunks"), 1, Integer.MAX_VALUE)
                || !text(data.get("name"), 255) || !text(data.get("type"), 255)
                || widget.getRootWidget().isEmpty()) {
            return false;
        }
        final String name = data.get("name").stringValue();
        final var options = widget.getRootWidget().orElseThrow().getOptions();
        final int size = data.get("size").intValue();
        final long chunks = Math.max(1L, ((long) size + options.getChunkSize() - 1)
            / options.getChunkSize());
        return !name.isEmpty() && name.indexOf('/') < 0 && name.indexOf('\\') < 0
            && name.indexOf('\0') < 0 && size <= options.getMaxFileSize()
            && chunks == data.get("totalChunks").intValue();
    }

    /**
     * Pointer events include keyboard-generated clicks (empty device type and zero pressure).
     */
    private static boolean pointerData(final JsonObject data) {
        if (!fields(data, "position", "type", "primary", "buttons", "keys", "pressure")
                || !text(data.get("type"), 5)
                || !Set.of("", "mouse", "touch", "pen").contains(data.get("type").stringValue())
                || !data.get("primary").isBoolean()
                || !integer(data.get("buttons"), 0, 63)
                || !number(data.get("pressure"), 0, 1)) {
            return false;
        }
        final JsonObject keys = data.get("keys").toJsonObject().orElse(null);
        final JsonObject position = data.get("position").toJsonObject().orElse(null);
        if (keys == null || !fields(keys, "ctrl", "alt", "shift", "meta")
                || position == null || !fields(position, "element", "client", "page", "screen")) {
            return false;
        }
        for (final String key : keys.keySet()) {
            if (!keys.get(key).isBoolean()) {
                return false;
            }
        }
        for (final String key : position.keySet()) {
            final JsonObject point = position.get(key).toJsonObject().orElse(null);
            if (point == null || !fields(point, "x", "y")
                    || !integer(point.get("x"), Integer.MIN_VALUE, Integer.MAX_VALUE)
                    || !integer(point.get("y"), Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies the standard pointer event family.
     */
    private static boolean pointer(final Event<?> event) {
        return event == Event.CLICK || event == Event.POINTER_ENTER || event == Event.POINTER_LEAVE
            || event == Event.POINTER_DOWN || event == Event.POINTER_UP;
    }

    /**
     * Rejects missing and extra fields, including nested arbitrary data.
     */
    private static boolean fields(final JsonObject object, final String... names) {
        return object.keySet().equals(Set.of(names));
    }

    /**
     * Checks strings without coercion.
     */
    private static boolean text(final JsonElement value, final int limit) {
        return value.isString() && value.stringValue().length() <= limit;
    }

    /**
     * Checks finite JSON numbers without coercion or overflow.
     */
    private static boolean number(final JsonElement value, final double min, final double max) {
        return value.isNumber() && Double.isFinite(value.doubleValue())
            && value.doubleValue() >= min && value.doubleValue() <= max;
    }

    /**
     * Checks integral values before calling intValue, which otherwise truncates fractions.
     */
    private static boolean integer(final JsonElement value, final int min, final int max) {
        if (!number(value, min, max)) {
            return false;
        }
        final java.math.BigDecimal exact = new java.math.BigDecimal(value.toString());
        return exact.stripTrailingZeros().scale() <= 0
            && exact.compareTo(java.math.BigDecimal.valueOf(min)) >= 0
            && exact.compareTo(java.math.BigDecimal.valueOf(max)) <= 0;
    }
}
