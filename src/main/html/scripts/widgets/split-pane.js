/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createSplitPane() {
    const pane = document.createElement("div");
    const handle = document.createElement("div");
    pane.className = "widgets-split-pane";
    handle.className = "widgets-split-handle";
    handle.setAttribute("role", "separator");
    pane.append(handle);
    let config;
    let horizontal = true;
    let disabled = false;
    let current = 50;
    let drag = null;

    function display(value) {
        current = Math.max(config.minimum, Math.min(config.maximum, Math.round(value)));
        pane.style.setProperty("--split-first", current + "fr");
        pane.style.setProperty("--split-second", 100 - current + "fr");
        handle.setAttribute("aria-valuenow", String(current));
        handle.setAttribute("aria-valuetext", current + "%");
    }

    function finishDrag(cancel) {
        if (!drag) return;
        const id = drag.id;
        drag = null;
        pane.classList.remove("dragging");
        if (handle.hasPointerCapture(id)) handle.releasePointerCapture(id);
        if (cancel) display(config.position);
    }

    function adapt() {
        if (!config || !pane.isConnected) return;
        const next = config.axis === "horizontal" && pane.clientWidth >= 480;
        if (next !== horizontal) finishDrag(true);
        horizontal = next;
        pane.classList.toggle("stacked", !horizontal);
        handle.setAttribute("aria-orientation", horizontal ? "vertical" : "horizontal");
    }

    function layout() {
        const areas = Array.from(pane.children).filter(child => child !== handle);
        pane.classList.toggle("single-area", areas.length < 2);
        handle.hidden = areas.length !== 2;
        if (handle.hidden) finishDrag(true);
        if (areas[0]) {
            areas[0].id ||= "widgets-split-area-" + areas[0]._id;
            handle.setAttribute("aria-controls", areas[0].id);
            if (areas[0].nextSibling !== handle) areas[0].after(handle);
        } else handle.removeAttribute("aria-controls");
    }

    pane._layoutChild = layout;
    pane._setSplitPane = state => {
        finishDrag(false);
        config = state;
        handle.setAttribute("aria-label", state.label);
        handle.setAttribute("aria-valuemin", String(state.minimum));
        handle.setAttribute("aria-valuemax", String(state.maximum));
        display(state.position);
        layout();
        adapt();
    };

    function available() {
        return config && !disabled && !handle.hidden && !pane._treePaused && pane.isConnected;
    }

    function coordinate(event) {
        return horizontal ? event.clientX : event.clientY;
    }

    function sign() {
        return horizontal && getComputedStyle(pane).direction === "rtl" ? -1 : 1;
    }

    function preview(event) {
        if (!drag || event.pointerId !== drag.id) return;
        const style = getComputedStyle(pane);
        const extent = horizontal
            ? pane.clientWidth -
              parseFloat(style.paddingLeft || 0) -
              parseFloat(style.paddingRight || 0) -
              handle.offsetWidth
            : pane.clientHeight -
              parseFloat(style.paddingTop || 0) -
              parseFloat(style.paddingBottom || 0) -
              handle.offsetHeight;
        if (extent > 0)
            display(drag.start + (sign() * 100 * (coordinate(event) - drag.offset)) / extent);
    }

    function commit() {
        sendEventToServer(pane, "split resize", { position: current });
    }

    listenWidget(
        pane,
        "pointerdown",
        event => {
            if (!available() || drag || !event.isPrimary || event.button !== 0) return;
            event.preventDefault();
            handle.focus({ preventScroll: true });
            handle.setPointerCapture(event.pointerId);
            drag = { id: event.pointerId, start: current, offset: coordinate(event) };
            pane.classList.add("dragging");
        },
        undefined,
        handle
    );
    listenWidget(pane, "pointermove", preview, undefined, handle);
    listenWidget(
        pane,
        "pointerup",
        event => {
            if (drag?.id !== event.pointerId) return;
            preview(event);
            const changed = current !== drag.start;
            finishDrag(false);
            if (changed) commit();
        },
        undefined,
        handle
    );
    for (const type of ["pointercancel", "lostpointercapture"])
        listenWidget(
            pane,
            type,
            event => {
                if (event.pointerId === drag?.id) finishDrag(true);
            },
            undefined,
            handle
        );

    listenWidget(
        pane,
        "keydown",
        event => {
            if (!available() || event.altKey || event.ctrlKey || event.metaKey) return;
            const step = event.shiftKey ? 10 : 1;
            let value;
            if (event.key === "Home") value = config.minimum;
            else if (event.key === "End") value = config.maximum;
            else if (event.key === (horizontal ? "ArrowRight" : "ArrowDown"))
                value = current + sign() * step;
            else if (event.key === (horizontal ? "ArrowLeft" : "ArrowUp"))
                value = current - sign() * step;
            else return;
            event.preventDefault();
            finishDrag(false);
            const previous = current;
            display(value);
            if (previous !== current) commit();
        },
        undefined,
        handle
    );

    Object.defineProperty(pane, "disabled", {
        get: () => disabled,
        set: value => {
            disabled = value;
            if (disabled) finishDrag(true);
            handle.tabIndex = disabled ? -1 : 0;
            handle.setAttribute("aria-disabled", String(disabled));
        }
    });
    handle.tabIndex = 0;
    const observer = new ResizeObserver(adapt);
    pane._onAttached = () => {
        observer.observe(pane);
        adapt();
    };
    pane._onDetached = pane._onDisposed = () => {
        observer.disconnect();
        finishDrag(true);
    };
    return pane;
}

function setSplitPane(data) {
    const widget = widgets[data.widget];
    const state = data.snapshot;
    if (
        !widget?._setSplitPane ||
        !state ||
        !["horizontal", "vertical"].includes(state.axis) ||
        typeof state.label !== "string" ||
        !state.label.trim() ||
        ![state.minimum, state.maximum, state.position].every(Number.isInteger) ||
        state.minimum < 0 ||
        state.maximum > 100 ||
        state.minimum >= state.maximum ||
        state.position < state.minimum ||
        state.position > state.maximum
    )
        return false;
    widget._setSplitPane(data.snapshot);
    return true;
}
