/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Canonical strings cross the wire; native controls own locale-specific display and pickers.
function createTemporalField(type) {
    const widget = createInputField();
    widget.type = type;
    widget.classList.add("widgets-temporal-field");
    if (type === "time") widget.step = "60";
    // Browsers expose incomplete native edits as empty value + badInput, not as raw text.
    // Keep that state distinct from an intentional clear on the authoritative server.
    widget._readText = () => (widget.validity.badInput ? "?" : widget.value);
    const setText = widget.setText;
    widget.setText = text => {
        if (text === "?") {
            // The browser owns incomplete segments; an echo must not erase that draft on blur.
            widget._deferredText = null;
            return false;
        }
        return setText(text);
    };
    return widget;
}

function setTemporalInput(data) {
    const widget = widgets[data.widget];
    const state = data.snapshot;
    if (!widget || !["date", "time"].includes(widget.type) || !widget._readText || !state)
        return false;
    const pattern = widget.type === "date" ? /^\d{4}-\d{2}-\d{2}$/ : /^\d{2}:\d{2}$/;
    if (
        typeof state.lang !== "string" ||
        !/^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$/.test(state.lang) ||
        ![state.min, state.max].every(
            value => typeof value === "string" && (value === "" || pattern.test(value))
        )
    )
        return false;
    widget.lang = state.lang;
    widget.min = state.min;
    widget.max = state.max;
    return true;
}
