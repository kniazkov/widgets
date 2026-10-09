/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Events stay queued until the server acknowledges their monotonically increasing IDs.
const events = [];
let lastEventId = 0;
let lastProcessedUpdateId = 0;

// IDs travel over the wire as "#<number>" strings.
function parseId(str) {
    if (typeof str !== "string" || !str.startsWith("#")) {
        throw new Error("Invalid ID format");
    }
    const num = Number(str.slice(1));
    if (!Number.isInteger(num) || num < 0) {
        throw new Error("Invalid numeric part of ID");
    }
    return num;
}

function createEvent(widget, type, data) {
    const eventId = "#" + ++lastEventId;
    const obj = {
        id: eventId,
        widget: widget._id,
        type: type
    };
    if (data) {
        obj.data = data;
    }
    log("The widget " + widget._id + " triggered the event " + eventId + " '" + type + "'.");
    events.push(obj);
}

// Dispatches updates in server order and tracks the last ID acknowledged by the browser.
function processUpdates(updates) {
    if (!updates || updates.length == 0) {
        return;
    }
    if (updates.length == 1) {
        log("Received 1 update.");
    } else {
        log("Received " + updates.length + " updates.");
    }
    for (let i = 0; i < updates.length && !clientDisposed; i++) {
        let result = false;
        const update = updates[i];
        const id = parseId(update.id);
        if (id <= lastProcessedUpdateId) {
            log("Update " + update.id + " skipped.");
            continue;
        }
        const handler = actionHandlers[update.action];
        if (handler) {
            result = handler(update);
            if (!result) {
                log("Update " + update.id + " was not processed due to incorrect data.");
            }
        } else {
            log("Unknown action: '" + update.action + "'.");
        }
        lastProcessedUpdateId = id;
    }
}

// Drops every queued event through the last ID acknowledged by the server.
function removeProcessedEvents(id) {
    let i;
    for (i = events.length - 1; i >= 0; i--) {
        if (events[i].id == id) {
            break;
        }
    }
    events.splice(0, i + 1);
}

// Reconciles delayed text updates after the server acknowledges local input events.
function reconcileTextInputs() {
    const pending = new Set(
        events.filter(event => event.type === "text input").map(event => event.widget)
    );
    for (const id in widgets) {
        const widget = widgets[id];
        if (typeof widget._applyDeferredText === "function") {
            widget._textInputPending = pending.has(widget._id);
            widget._applyDeferredText();
        }
    }
}

// These wire names must match the update actions serialized by the Java server.
const actionHandlers = {
    "create widget": createWidget,
    reset: reset,
    "clear page cache": clearPageCache,
    "go to page": goToPage,
    "open page in new tab": openPageInNewTab,
    subscribe: subscribeToEvent,
    "set client event action": setClientEventAction,
    "set child": setChildWidget,
    "append child": appendChildWidget,
    "insert child": insertChildWidget,
    "remove child": removeChildWidget,
    "set valid": setValidFlag,
    "set disabled": setDisabledFlag,
    "set hidden": setHiddenFlag,
    "set text": setText,
    "set suggestions": setSuggestions,
    "set placeholder color": setPlaceholderColor,
    "set placeholder": setPlaceholder,
    "set document title": data => setDocumentMetadata(data, "title"),
    "set document description": data => setDocumentMetadata(data, "description"),
    "set document robots": data => setDocumentMetadata(data, "robots"),

    "set input mode": setInputMode,
    "set suggestion separator": setSuggestionSeparator,
    "set options": setOptions,
    "set option": setOption,
    "set carousel sources": setCarouselSources,
    "set carousel source": setCarouselSource,
    "set selected index": setSelectedIndex,
    "set child order": setChildOrder,
    "set max scale": setMaxScale,
    "set fit to first child": setFitToFirstChild,
    "set fit content": setFitContent,
    "set animation duration": setAnimationDuration,
    "reset zoom": resetZoom,
    "set href": setHref,
    "set color": setColor,
    "set bg color": setBgColor,
    "set backdrop color": setBackdropColor,
    "set close on outside click": setCloseOnOutsideClick,
    "set opacity": setOpacity,
    "set width": setWidth,
    "set max width": setMaxWidth,
    "set max height": setMaxHeight,
    "set height": setHeight,
    "set margin": setMargin,
    "set padding": setPadding,
    "set font face": setFontFace,
    "set font size": setFontSize,
    "set font weight": setFontWeight,
    "set italic": setItalic,
    "set text decoration": setTextDecoration,
    "set border color": setBorderColor,
    "set border style": setBorderStyle,
    "set border width": setBorderWidth,
    "set border radius": setBorderRadius,
    "set box shadow": setBoxShadow,
    "set outline": setOutline,
    "set cursor": setCursor,
    "set transition": setTransition,
    "set box sizing": setBoxSizing,
    "set overflow": setOverflow,
    "set source": setSource,
    "set intrinsic size": setIntrinsicSize,
    "set sel source": setSelectedSource,
    "set unsel source": setUnselectedSource,
    "set horz alignment": setHorzAlignment,
    "set vert alignment": setVertAlignment,
    "set sticky side": setStickySide,
    "set cell spacing": setCellSpacing,
    "set checked": setCheckedFlag,
    "set multiple input": setMultipleInput,
    "set accepted files": setAcceptedFiles
};

// These events are client-side protocol primitives and do not require an explicit subscription.
const ALWAYS_ALLOWED_EVENTS = ["text input", "check", "select", "upload", "reorder", "dismiss"];

function sendEventToServer(widget, type, data) {
    if (clientDisposed) {
        return;
    }
    const clientAction = widget._clientActions[type];
    if (clientAction) {
        const handler = actionHandlers[clientAction.action];
        if (handler) {
            handler(clientAction);
        } else {
            log("Unknown client action: '" + clientAction.action + "'.");
        }
    }
    if (widget._events[type] || ALWAYS_ALLOWED_EVENTS.includes(type)) {
        createEvent(widget, type, data);
        sendSynchronizeRequest();
    }
}
