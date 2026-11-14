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
    listenWidget(widget, "input", function () {
        widget._textInputPending = true;
        sendEventToServer(widget, "text input", { text: widget.value });
    });
    listenWidget(widget, "blur", function () {
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

// One listener per phase and editor; subsequent updates only replace the explicit table.
function setKeyboardShortcuts(data) {
    const widget = widgets[data.widget];
    const shortcuts = data.shortcuts;
    const key =
        /^(DOWN|UP):(Enter|Escape|Arrow(?:Up|Down|Left|Right)|Home|End|Page(?:Up|Down)):[01]{4}:[01]$/;
    if (
        !widget?._applyDeferredText ||
        !Array.isArray(shortcuts) ||
        shortcuts.length > 32 ||
        !shortcuts.every(entry => typeof entry === "string" && key.test(entry))
    ) {
        return false;
    }
    const next = new Map(shortcuts.map(entry => [entry.slice(0, -2), entry.endsWith(":1")]));
    if (next.size !== shortcuts.length) return false;
    if (!widget._keyboardInitialized && shortcuts.length) initKeyboardShortcuts(widget);
    widget._keyboardShortcuts = next;
    return true;
}

function setInputReadOnly(data) {
    const widget = widgets[data.widget];
    if (!widget?._applyDeferredText || typeof data["read only"] !== "boolean") return false;
    widget.readOnly = data["read only"];
    if (widget.readOnly) widget._closeSuggestions?.();
    return true;
}

function initKeyboardShortcuts(widget) {
    widget._keyboardInitialized = true;
    let composing = false;
    let consumedEnter = false;
    listenWidget(widget, "compositionstart", () => {
        composing = true;
    });
    listenWidget(widget, "compositionend", () => {
        composing = false;
    });
    listenWidget(widget, "blur", () => {
        composing = false;
        consumedEnter = false;
    });

    function send(event, phase) {
        if (
            event.target !== widget ||
            !widget.isConnected ||
            widget._treePaused ||
            widget.disabled ||
            widget.readOnly ||
            widget.closest("[hidden]")
        ) {
            return;
        }
        if (event.key === "Enter" && phase === "DOWN" && !event.repeat) {
            consumedEnter =
                event.defaultPrevented ||
                composing ||
                event.isComposing ||
                event.keyCode === 229 ||
                widget._isSuggestionOpen?.() === true;
        }
        const enterWasConsumed = event.key === "Enter" && consumedEnter;
        if (event.key === "Enter" && phase === "UP") consumedEnter = false;
        if (
            event.defaultPrevented ||
            composing ||
            event.isComposing ||
            event.keyCode === 229 ||
            enterWasConsumed
        )
            return;

        const { key, ctrlKey, altKey, shiftKey, metaKey } = event;
        const id = `${phase}:${key}:${+ctrlKey}${+altKey}${+shiftKey}${+metaKey}`;
        if (!widget._keyboardShortcuts?.has(id)) return;
        if (widget._keyboardShortcuts.get(id)) {
            event.preventDefault();
            event.stopPropagation();
        }
        if (event.repeat) return;
        // Earlier input notifications are already in this same acknowledged ordered queue.
        if (
            createEvent(widget, "keyboard", {
                key,
                phase,
                ctrl: ctrlKey,
                alt: altKey,
                shift: shiftKey,
                meta: metaKey
            })
        )
            sendSynchronizeRequest();
    }
    listenWidget(widget, "keydown", event => send(event, "DOWN"));
    listenWidget(widget, "keyup", event => send(event, "UP"));
}

function setFormEditorAttribute(data, property) {
    const widget = widgets[data.widget];
    const value = data[property];
    if (!widget?._applyDeferredText) return false;
    if (property === "required" && typeof value === "boolean") {
        widget.required = value;
    } else if (
        property === "max length" &&
        Number.isInteger(value) &&
        (value === -1 || (value >= 1 && value <= 65536))
    ) {
        if (value < 0) widget.removeAttribute("maxlength");
        else widget.maxLength = value;
    } else if (
        property === "form name" &&
        typeof value === "string" &&
        (value === "" || /^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$/.test(value))
    ) {
        assignSemanticAttribute(widget, "name", value);
    } else if (
        property === "autocomplete" &&
        typeof value === "string" &&
        /^[a-zA-Z0-9_ -]{0,128}$/.test(value)
    ) {
        assignSemanticAttribute(widget, "autocomplete", value);
    } else {
        return false;
    }
    return true;
}

function setFormEditorError(data) {
    const widget = widgets[data.widget];
    const flag = data["form error"];
    if (!widget?._applyDeferredText || typeof flag !== "boolean") return false;
    widget._formError = flag;
    refreshEditorValidity(widget);
    return true;
}

function setFormBusy(data) {
    const widget = widgets[data.widget];
    const flag = data["form busy"];
    if (widget?._widgetType !== "form" || typeof flag !== "boolean") return false;
    assignSemanticAttribute(widget, "aria-busy", flag ? "true" : null);
    return true;
}

function setFormStatus(data) {
    const widget = widgets[data.widget];
    const flag = data["form status"];
    if (widget?._widgetType !== "text" || typeof flag !== "boolean") return false;
    assignSemanticAttribute(widget, "role", flag ? "status" : null);
    if (flag) widget.classList.add("widgets-form-status");
    return true;
}

function focusFormEditor(data) {
    const widget = widgets[data.widget];
    if (!widget?._applyDeferredText) return false;
    if (
        widget.isConnected &&
        !widget._treePaused &&
        !widget.disabled &&
        !widget.closest("[hidden]")
    ) {
        widget.focus();
    }
    return true;
}
