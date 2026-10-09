/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Browser event handling and serialization.
function createInputField() {
    const widget = document.createElement("input");
    initEditableText(widget);
    initPointerEvents(widget);
    initFocusEvents(widget, "active");
    initTextAlignment(widget);
    return widget;
}

// Protects local editing from delayed server echoes and applies the latest value when safe.
function initEditableText(widget) {
    widget._deferredText = null;
    widget._textInputPending = false;
    widget.setText = function (text) {
        if (document.activeElement === widget || widget._textInputPending) {
            widget._deferredText = text;
            return false;
        }
        widget._deferredText = null;
        if (widget.value != text) {
            widget.value = text;
            return true;
        }
        return false;
    };
    widget._applyDeferredText = function () {
        if (
            document.activeElement === widget ||
            widget._textInputPending ||
            widget._deferredText === null
        ) {
            return false;
        }
        const text = widget._deferredText;
        widget._deferredText = null;
        if (widget.value != text) {
            widget.value = text;
            return true;
        }
        return false;
    };
    addEvent(widget, "input", function () {
        widget._textInputPending = true;
        sendEventToServer(widget, "text input", { text: widget.value });
    });
    addEvent(widget, "blur", function () {
        widget._applyDeferredText();
    });
}

// Adds CSS text alignment support to text input controls.
function initTextAlignment(widget) {
    widget._setHorzAlignment = function (value) {
        switch (value) {
            case "center":
            case "right":
            case "justify":
                widget.style.textAlign = value;
                break;
            default:
                widget.style.textAlign = "left";
                break;
        }
    };
}

// Presentation hints never change the field value or validation.
function setPlaceholder(data) {
    const widget = widgets[data.widget];
    if (!widget || !("placeholder" in widget) || typeof data.placeholder !== "string") return false;
    widget.placeholder = data.placeholder;
    return true;
}

function setInputMode(data) {
    const widget = widgets[data.widget];
    const mode = data["input mode"];
    if (
        !widget ||
        !["text", "numeric", "decimal", "tel", "email", "url", "search", "none"].includes(mode)
    )
        return false;
    widget.inputMode = mode;
    return true;
}

function setPlaceholderColor(data) {
    const widget = widgets[data.widget];
    const color = data["placeholder color"];
    if (!widget || !("placeholder" in widget) || !color || typeof color !== "object") return false;
    widget.style.setProperty("--widgets-placeholder-color", composeColor(color));
    return true;
}
