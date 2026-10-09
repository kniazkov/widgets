/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function subscribeToEvent(data) {
    const widget = widgets[data.widget];
    const event = data.event;
    if (widget && event) {
        log("Server subscribed to the '" + event + "' event of widget " + widget._id + ".");
        widget._events[event] = true;
    }
}

function setClientEventAction(data) {
    const widget = widgets[data.widget];
    const event = data.event;
    const clientAction = data.clientAction;
    if (
        widget &&
        typeof event == "string" &&
        clientAction &&
        typeof clientAction.action == "string"
    ) {
        widget._clientActions[event] = clientAction;
        log("A client action was set for the '" + event + "' event of widget " + widget._id + ".");
        return true;
    }
    return false;
}

function processPointerEvent(element, event) {
    const rect = element.getBoundingClientRect();
    const data = {};
    data.position = {};
    data.position.element = {
        x: Math.round(event.clientX - rect.left),
        y: Math.round(event.clientY - rect.top)
    };
    data.position.client = {
        x: Math.round(event.clientX),
        y: Math.round(event.clientY)
    };
    data.position.page = {
        x: Math.round(event.pageX),
        y: Math.round(event.pageY)
    };
    data.position.screen = {
        x: Math.round(event.screenX),
        y: Math.round(event.screenY)
    };
    data.type = event.pointerType;
    data.primary = event.isPrimary;
    data.buttons = event.buttons;
    data.keys = {
        ctrl: event.ctrlKey,
        alt: event.altKey,
        shift: event.shiftKey,
        meta: event.metaKey
    };
    data.pressure = event.pressure;
    return data;
}

function initPointerEvents(widget, activeOnPointerDown) {
    addEvent(widget, "click", function (event) {
        if (
            widget.tagName === "A" &&
            (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)
        ) {
            return;
        }
        if (widget.tagName === "A" && widget._clientActions.click?.action === "go to page") {
            event.preventDefault();
        }
        if (widget._suppressClick) {
            widget._suppressClick = false;
            return;
        }
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
        if (widget._onClick) {
            widget._onClick();
        }
    });
    addEvent(widget, "pointerenter", function (event) {
        widget._states.hovered = true;
        if (widget._events.click || widget._clientActions.click) {
            widget.style.cursor = "pointer";
        }
        refreshWidget(widget);
        sendEventToServer(widget, "pointer enter", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerleave", function (event) {
        if (activeOnPointerDown) {
            widget._states.active = false;
        }
        widget._states.hovered = false;
        if (widget._events.click || widget._clientActions.click) {
            widget.style.cursor = "default";
        }
        refreshWidget(widget);
        sendEventToServer(widget, "pointer leave", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerdown", function (event) {
        if (activeOnPointerDown) {
            widget._states.active = true;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer down", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerup", function (event) {
        if (activeOnPointerDown) {
            widget._states.active = false;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer up", processPointerEvent(widget, event));
    });
}

function initFocusEvents(widget) {
    addEvent(widget, "focus", function () {
        widget._states.focused = true;
        refreshWidget(widget);
        sendEventToServer(widget, "focus", {});
    });
    addEvent(widget, "blur", function () {
        widget._states.focused = false;
        refreshWidget(widget);
        sendEventToServer(widget, "blur", {});
    });
}

// Suppress only clicks produced by a gesture, including clicks on nested controls.
function initGestureClicks(widget) {
    let suppressed = false;
    widget._suppressGestureClick = () => {
        suppressed = true;
    };
    widget.addEventListener(
        "pointerdown",
        () => {
            suppressed = false;
        },
        true
    );
    widget.addEventListener(
        "click",
        event => {
            if (suppressed) {
                suppressed = false;
                event.preventDefault();
                event.stopImmediatePropagation();
            }
        },
        true
    );
    widget.addEventListener("dragstart", event => event.preventDefault());
}

function releaseGesturePointer(widget, id) {
    if (widget.hasPointerCapture?.(id)) widget.releasePointerCapture(id);
}
