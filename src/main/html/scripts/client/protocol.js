/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Events stay queued until the server acknowledges their monotonically increasing IDs.
const events = [];
let lastEventId = 0;
let lastAcknowledgedEventId = 0;
let lastAppliedUpdateSequence = 0;
const updateStreamVersion = "2";
const updateStreamCapability = "sequenced-updates";
const updateOutcome = Object.freeze({ applied: "applied", noop: "noop", failure: "failure" });

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

// An independent unsent queue is bounded even while the transport is unavailable.
const maxPendingEvents = 256;
const maxPendingEventBytes = 512 * 1024;
const maxPendingEventAge = 120_000;
const eventCharges = new WeakMap();
let pendingEventBytes = 0;
let eventBatchCount = 64;
let eventBatchBytes = 512 * 1024;

function protocolEventBytes(value) {
    let bytes = 0;
    for (const codepoint of value) {
        const code = codepoint.codePointAt(0);
        bytes += code <= 127 ? 1 : code <= 2047 ? 2 : code <= 65535 ? 3 : 4;
    }
    return bytes;
}

function eventCharge(event) {
    return eventCharges.get(event)?.bytes ?? protocolEventBytes(JSON.stringify(event)) + 1;
}

function configureEventBatches(count, bytes) {
    if (Number.isInteger(count) && count >= 1 && count <= 128) eventBatchCount = count;
    if (Number.isInteger(bytes) && bytes >= 16 && bytes <= 512 * 1024) eventBatchBytes = bytes;
}

function failEventQueue(reason) {
    console.warn("Widgets event queue stopped", reason, events.length, pendingEventBytes);
    events.length = 0;
    pendingEventBytes = 0;
    showClientError(
        undefined,
        "backpressure",
        "Input queue could not be delivered safely. Reload this page; unsent actions are not replayed."
    );
}

function selectEventBatch() {
    const since = events[0] && eventCharges.get(events[0])?.at;
    if (since != null && Date.now() - since >= maxPendingEventAge) {
        failEventQueue("age");
        return null;
    }
    let bytes = 2;
    const selected = [];
    for (const event of events) {
        const charge = eventCharge(event);
        if (selected.length >= eventBatchCount || bytes + charge > eventBatchBytes) break;
        selected.push(event);
        bytes += charge;
    }
    return selected;
}

function createEvent(widget, type, data) {
    if (typeof clientDisposed !== "undefined" && (clientDisposed || clientFailed)) return false;
    const eventId = "#" + (lastEventId + 1);
    const obj = { id: eventId, widget: widget._id, type };
    if (data) obj.data = data;
    const bytes = eventCharge(obj);
    if (
        bytes + 2 > eventBatchBytes ||
        events.length >= maxPendingEvents ||
        pendingEventBytes + bytes > maxPendingEventBytes
    ) {
        failEventQueue("capacity");
        return false;
    }
    lastEventId++;
    eventCharges.set(obj, { bytes, at: Date.now() });
    pendingEventBytes += bytes;
    events.push(obj);
    log("The widget " + widget._id + " triggered the event " + eventId + " '" + type + "'.");
    return true;
}

// A rejected batch stops the runtime. Only a fresh channel can supply a new snapshot.
function processUpdates(receipt, modalActivation = null) {
    if (clientDisposed || clientFailed) return false;
    try {
        validateUpdateEnvelope(receipt);
        for (const update of receipt.updates) {
            if (clientDisposed) break;
            if (update.sequence <= lastAppliedUpdateSequence) continue;
            const outcome = applyUpdate(update);
            if (outcome === updateOutcome.failure) {
                throw new Error("Unapplied widget update at sequence " + update.sequence);
            }
            lastAppliedUpdateSequence = update.sequence;
        }
        if (!clientDisposed) {
            trimDetachedWidgets();
            refreshModalStack(modalActivation);
        }
        return !clientDisposed;
    } catch (error) {
        showUpdateProtocolError(error);
        return false;
    }
}

// Validate the entire range before any DOM mutation. Delayed replies may replay older prefixes.
function validateUpdateEnvelope(receipt) {
    if (!updateEpoch || receipt.updateEpoch !== updateEpoch) {
        throw new Error("Unexpected widget update epoch");
    }
    const { updateBase, updateThrough, snapshot, updates } = receipt;
    if (
        !Number.isSafeInteger(updateBase) ||
        updateBase < 0 ||
        updateBase > lastAppliedUpdateSequence ||
        !Number.isSafeInteger(updateThrough) ||
        updateThrough < updateBase ||
        snapshot !== (updateBase === 0) ||
        !Array.isArray(updates) ||
        updates.length !== updateThrough - updateBase
    ) {
        throw new Error("Invalid widget update checkpoint");
    }
    let expected = updateBase;
    for (const update of updates) {
        if (
            !update ||
            !Number.isSafeInteger(update.sequence) ||
            update.sequence !== ++expected ||
            typeof update.action !== "string"
        ) {
            throw new Error("Missing or reordered widget update");
        }
    }
}

// Existing widget handlers return boolean; wire dispatch distinguishes intentional no-ops.
function applyUpdate(update) {
    if (!Object.hasOwn(actionHandlers, update.action)) return updateOutcome.failure;
    if (
        (update.action === "go to page" || update.action === "open page in new tab") &&
        typeof update.href !== "string"
    )
        return updateOutcome.failure;
    const widget = widgets[update.widget];
    if (update.action === "create widget" && widget) {
        return widget._widgetType === update.type ? updateOutcome.noop : updateOutcome.failure;
    }
    if (update.action === "dispose widget") {
        if (!validUpdateWidgetId(update.widget)) return updateOutcome.failure;
        if (!widget) return updateOutcome.noop;
    }
    if (update.action === "remove child") {
        if (!validUpdateWidgetId(update.widget) || !validUpdateWidgetId(update.container)) {
            return updateOutcome.failure;
        }
        const former = widgets[update.container];
        if (!widget || !former || widget.parentNode !== (former._childHost || former)) {
            return updateOutcome.noop;
        }
    }
    return actionHandlers[update.action](update) === true
        ? updateOutcome.applied
        : updateOutcome.failure;
}

function validUpdateWidgetId(id) {
    return typeof id === "string" && /^#[1-9][0-9]*$/.test(id);
}

// Drops every queued event through the last ID acknowledged by the server.
function removeProcessedEvents(id, sentThrough = lastEventId) {
    if (typeof id !== "string" || (id !== "#?" && !/^#(0|[1-9][0-9]*)$/.test(id))) {
        throw new Error("Invalid event acknowledgement");
    }
    const ack = id === "#?" ? 0 : parseId(id);
    if (!Number.isSafeInteger(ack) || ack < lastAcknowledgedEventId || ack > sentThrough) {
        throw new Error("Invalid event acknowledgement");
    }
    let count = 0;
    while (count < events.length && parseId(events[count].id) <= ack) {
        pendingEventBytes -= eventCharges.get(events[count])?.bytes ?? 0;
        count++;
    }
    events.splice(0, count);
    lastAcknowledgedEventId = ack;
    return count;
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
    "download file": downloadFile,
    subscribe: subscribeToEvent,
    "set client event action": setClientEventAction,
    "set child": setChildWidget,
    "append child": appendChildWidget,
    "insert child": insertChildWidget,
    "remove child": removeChildWidget,
    "dispose widget": disposeWidget,
    "set valid": setValidFlag,
    "set disabled": setDisabledFlag,
    "set hidden": setHiddenFlag,
    "set countdown deadline": setCountdownDeadline,
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
    "set close on escape": setCloseOnEscape,
    "set accessible description": setWidgetAccessibleDescription,
    "set accessible label": data => setAccessibleReference(data, "accessible label"),
    "set accessible description widget": data =>
        setAccessibleReference(data, "accessible description widget"),
    "set accessible error": data => setAccessibleReference(data, "accessible error"),
    "set heading level": setHeadingLevel,
    "set alternative text": setAlternativeText,
    "set decorative image": setDecorativeImage,
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
    "set accessible name": setWidgetAccessibleName,
    "set radio group": setRadioGroupName,
    "set multiple input": setMultipleInput,
    "set accepted files": setAcceptedFiles
};

// These events are client-side protocol primitives and do not require an explicit subscription.
const ALWAYS_ALLOWED_EVENTS = [
    "text input",
    "check",
    "select",
    "upload",
    "reorder",
    "dismiss",
    "dismiss by escape"
];

function sendEventToServer(widget, type, data) {
    if (clientDisposed || clientFailed) {
        return;
    }
    const clientAction = widget._clientActions[type];
    if (clientAction) {
        const handler = actionHandlers[clientAction.action];
        if (handler) {
            handler(clientAction);
            refreshModalStack(widget);
        } else {
            log("Unknown client action: '" + clientAction.action + "'.");
        }
    }
    if (widget._events[type] || ALWAYS_ALLOWED_EVENTS.includes(type)) {
        createEvent(widget, type, data);
        sendSynchronizeRequest();
    }
}
