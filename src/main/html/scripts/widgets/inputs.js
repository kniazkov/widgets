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
    widget._unsentText = false;
    widget._composingText = false;
    widget._inputDebounce = 0;
    let timer = null;
    let lastSent = widget.value;
    const cancelTimer = () => {
        clearTimeout(timer);
        timer = null;
    };
    const protectedEdit = () =>
        document.activeElement === widget ||
        widget._textInputPending ||
        widget._unsentText ||
        widget._composingText;
    widget.setText = function (text) {
        if (
            widget._unsentText ||
            widget._composingText ||
            (typeof lastAcknowledgedEventId !== "undefined" &&
                widget._lastTextEventId > lastAcknowledgedEventId)
        )
            return false;
        if (protectedEdit()) {
            widget._deferredText = text;
            return false;
        }
        widget._deferredText = null;
        lastSent = text;
        if (widget.value != text || (text === "" && widget.validity?.badInput)) {
            widget.value = text;
            return true;
        }
        return false;
    };
    widget._applyDeferredText = function () {
        if (protectedEdit() || widget._deferredText === null) return false;
        return widget.setText(widget._deferredText);
    };
    widget._flushTextInput = () => {
        cancelTimer();
        if (
            !widget._unsentText ||
            widget._composingText ||
            widget._treePaused ||
            !widget.isConnected ||
            widget.disabled ||
            widget.readOnly
        )
            return;
        widget._unsentText = false;
        const text = widget._readText ? widget._readText() : widget.value;
        if (text === lastSent) return;
        lastSent = text;
        widget._textInputPending = true;
        sendEventToServer(widget, "text input", { text });
    };
    const edited = () => {
        widget._unsentText = true;
        // An answer received before this edit cannot become authoritative on a later blur.
        widget._deferredText = null;
        cancelTimer();
        if (widget._composingText) return;
        if (widget._inputDebounce && document.activeElement === widget) {
            timer = setTimeout(widget._flushTextInput, widget._inputDebounce);
        } else widget._flushTextInput();
    };
    listenWidget(widget, "compositionstart", () => {
        widget._composingText = true;
        cancelTimer();
    });
    listenWidget(widget, "compositionupdate", () => {
        widget._composingText = true;
    });
    listenWidget(widget, "compositionend", () => {
        widget._composingText = false;
        edited();
    });
    listenWidget(widget, "input", event => {
        if (event.isComposing) widget._composingText = true;
        edited();
    });
    listenWidget(widget, "blur", () => {
        widget._flushTextInput();
        widget._applyDeferredText();
    });
    const detached = widget._onDetached;
    widget._onDetached = () => {
        cancelTimer();
        detached?.();
    };
    const attached = widget._onAttached;
    widget._onAttached = () => {
        attached?.();
        widget._flushTextInput();
    };
    const disposed = widget._onDisposed;
    widget._onDisposed = () => {
        cancelTimer();
        widget._unsentText = false;
        widget._deferredText = null;
        disposed?.();
    };
}

function setInputDebounce(data) {
    const widget = widgets[data.widget];
    if (
        !widget?._flushTextInput ||
        !Number.isInteger(data.milliseconds) ||
        data.milliseconds < 0 ||
        data.milliseconds > 2000
    )
        return false;
    widget._inputDebounce = data.milliseconds;
    widget._flushTextInput();
    return true;
}

function flushTextInputs() {
    for (const widget of Object.values(widgets)) widget._flushTextInput?.();
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
        flushTextInputs();
        // Flush committed edits before the keyboard command in the same ordered queue.
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
