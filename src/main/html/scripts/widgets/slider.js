/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createSlider() {
    const widget = document.createElement("div");
    const input = document.createElement("input");
    const value = document.createElement("span");
    input.type = "range";
    input.min = "0";
    input.step = "1";
    widget.className = "widgets-slider";
    widget._semanticTargetElement = input;
    value.setAttribute("aria-hidden", "true");
    widget.append(input, value);
    let state;
    let committed = 0;
    let pointer = null;
    let key = null;

    function decimal(index) {
        const number = BigInt(state.base) + BigInt(index) * BigInt(state.step);
        const sign = number < 0n ? "-" : "";
        const digits = (number < 0n ? -number : number).toString().padStart(state.scale + 1, "0");
        return (
            sign +
            (state.scale
                ? digits.slice(0, -state.scale) + "." + digits.slice(-state.scale)
                : digits)
        );
    }

    function preview() {
        const number = decimal(input.valueAsNumber);
        const text = state.unit ? number + " " + state.unit : number;
        input.setAttribute("aria-valuenow", number);
        input.setAttribute("aria-valuetext", text);
        value.textContent = text;
    }

    function cancel() {
        pointer = null;
        key = null;
        if (state) {
            input.valueAsNumber = committed;
            preview();
        }
    }

    function commit() {
        if (!state || input.disabled || widget._treePaused || !widget.isConnected) return;
        preview();
        const index = input.valueAsNumber;
        if (index !== committed) {
            committed = index;
            sendEventToServer(widget, "slider commit", { index });
        }
    }

    widget._setSlider = snapshot => {
        state = snapshot;
        committed = snapshot.index;
        input.max = String(state.ticks);
        input.setAttribute("aria-valuemin", decimal(0));
        input.setAttribute("aria-valuemax", decimal(state.ticks));
        if (pointer === null && key === null) {
            input.valueAsNumber = committed;
            preview();
        }
    };

    Object.defineProperty(widget, "disabled", {
        get: () => input.disabled,
        set: disabled => {
            if (disabled) cancel();
            input.disabled = disabled;
        }
    });
    widget._onDetached = widget._onDisposed = cancel;

    function listen(type, listener) {
        listenWidget(widget, type, listener, undefined, input);
    }
    listen("pointerdown", event => {
        if (!state || input.disabled || !event.isPrimary || event.button !== 0) return;
        pointer = event.pointerId;
        // Leave native thumb tracking to the browser, including its internal pointer capture.
    });
    listen("input", () => {
        if (state) preview();
    });
    listen("change", () => {
        if (pointer === null && key === null) commit();
    });
    listenWidget(
        widget,
        "pointerup",
        event => {
            if (pointer !== event.pointerId) return;
            pointer = null;
            commit();
        },
        undefined,
        window
    );
    listenWidget(
        widget,
        "pointercancel",
        event => {
            if (pointer === event.pointerId) cancel();
        },
        true,
        window
    );
    listenWidget(widget, "blur", cancel, undefined, window);

    listen("keydown", event => {
        if (!state || input.disabled || event.altKey || event.ctrlKey || event.metaKey) return;
        if (event.key === "Escape") {
            if (pointer !== null || key !== null) cancel();
            return;
        }
        const page = Math.max(1, Math.round(state.ticks / 10));
        let next = input.valueAsNumber;
        switch (event.key) {
            case "ArrowRight":
                next += getComputedStyle(input).direction === "rtl" ? -1 : 1;
                break;
            case "ArrowLeft":
                next += getComputedStyle(input).direction === "rtl" ? 1 : -1;
                break;
            case "ArrowUp":
                next++;
                break;
            case "ArrowDown":
                next--;
                break;
            case "Home":
                next = 0;
                break;
            case "End":
                next = state.ticks;
                break;
            case "PageUp":
                next += page;
                break;
            case "PageDown":
                next -= page;
                break;
            default:
                return;
        }
        event.preventDefault();
        if (pointer !== null) return;
        key = event.key;
        input.valueAsNumber = Math.max(0, Math.min(state.ticks, next));
        preview();
    });
    listen("keyup", event => {
        if (key !== event.key) return;
        key = null;
        commit();
    });
    listen("blur", () => {
        if (key !== null) {
            key = null;
            commit();
        }
    });
    return widget;
}

function setSlider(data) {
    const state = data.snapshot;
    const widget = widgets[data.widget];
    if (
        !widget?._setSlider ||
        !state ||
        !/^-?\d{1,36}$/.test(state.base) ||
        !/^[1-9]\d{0,35}$/.test(state.step) ||
        !Number.isInteger(state.scale) ||
        state.scale < 0 ||
        state.scale > 18 ||
        !Number.isInteger(state.ticks) ||
        state.ticks < 1 ||
        state.ticks > 1000000 ||
        !Number.isInteger(state.index) ||
        state.index < 0 ||
        state.index > state.ticks ||
        typeof state.unit !== "string"
    )
        return false;
    widget._setSlider(state);
    return true;
}
