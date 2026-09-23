/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

// Each runtime factory owns these variables, its widget registry and upload scheduler.
const pageContext = typeof page === "undefined" ? null : page;
let clientDisposed = false;
let synchronizationInFlight = false;
const synchronizationCallbacks = [];
let clientId = null;
let serverId = null;
let browserId = null;
const period = 2500;
let mainCycleTask = null;
const maxConsecutiveRequestFailures = 3;
const connectionOverlayId = "connection-terminated-overlay";
const clientErrorOverlayId = "client-error-overlay";
let consecutiveRequestFailures = 0;
let reloadRequested = false;
let clientFailed = false;
let clientCreationInProgress = false;

// Events stay in this queue until the server acknowledges their monotonically increasing IDs.
const events = [];
let lastEventId = 0;
let lastProcessedUpdateId = 0;

// A persistent transport failure blocks interaction without discarding the current page state.
function showConnectionTerminated() {
    if (
        clientDisposed ||
        clientFailed ||
        (pageContext ? pageContext.root : document).querySelector("#" + connectionOverlayId)
    ) {
        return;
    }
    const overlay = document.createElement("div");
    overlay.id = connectionOverlayId;
    overlay.textContent = "Connection Terminated";
    (pageContext ? pageContext.root : document.body || document.documentElement).appendChild(
        overlay
    );
    if (pageContext) {
        pageContext.failure(overlay);
    }
}

function hideConnectionTerminated() {
    const overlay = (pageContext ? pageContext.root : document).querySelector(
        "#" + connectionOverlayId
    );
    if (overlay) {
        overlay.remove();
    }
}

// A client failure is fatal for the current page and must not be retried as a network failure.
function showClientError(error) {
    if (clientDisposed || clientFailed) {
        return;
    }
    clientFailed = true;
    clearInterval(mainCycleTask);
    hideConnectionTerminated();
    if (error) {
        console.error("Client error", error);
        if (clientId) {
            try {
                const summary = error.message
                    ? String(error.name || "Error") + ": " + String(error.message)
                    : String(error);
                const detail = (summary + (error.stack ? "\n" + String(error.stack) : "")).slice(
                    0,
                    8192
                );
                sendRequest(
                    { action: "report error", client: clientId, error: detail },
                    null,
                    "post"
                );
            } catch {
                // Reporting must never prevent the original failure from being displayed.
            }
        }
    }
    const overlay = document.createElement("div");
    overlay.id = clientErrorOverlayId;
    overlay.textContent = "Client Error";
    (pageContext ? pageContext.root : document.body || document.documentElement).appendChild(
        overlay
    );
    if (pageContext) {
        pageContext.failure(overlay);
    }
}

function responseHasClientError(response) {
    if (response && response.clientError === true) {
        showClientError();
        return true;
    }
    return false;
}

function recordRequestFailure() {
    if (clientDisposed || clientFailed) {
        return;
    }
    consecutiveRequestFailures++;
    if (consecutiveRequestFailures >= maxConsecutiveRequestFailures) {
        showConnectionTerminated();
    }
}

function recordRequestSuccess() {
    consecutiveRequestFailures = 0;
    hideConnectionTerminated();
    if (pageContext) {
        pageContext.recovered();
    }
}

// Reloading the current location preserves both the page path and its query parameters.
function reloadCurrentPage() {
    if (reloadRequested) {
        return;
    }
    reloadRequested = true;
    clearInterval(mainCycleTask);
    if (pageContext) {
        pageContext.expire();
    } else {
        window.location.reload();
    }
}

function serverStateIsCurrent(response) {
    if (response.serverId !== serverId || response.clientAlive !== true) {
        reloadCurrentPage();
        return false;
    }
    return true;
}

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

function initClient(sessionId, address, data) {
    browserId = localStorage.getItem("browserId");
    if (!browserId) {
        browserId = sessionId;
        localStorage.setItem("browserId", browserId);
    }
    if (!pageContext) {
        window.addEventListener("beforeunload", function () {
            if (clientId != null) {
                sendRequest({
                    action: "kill",
                    client: clientId
                });
            }
        });
    }
    startClient(address, data);
}

function retryClientCreation(address, data) {
    setTimeout(function () {
        startClient(address, data);
    }, 1000);
}

function startClient(address, data) {
    if (clientDisposed || clientFailed || clientId != null || clientCreationInProgress) {
        return;
    }
    clientCreationInProgress = true;
    const request = { ...data };
    request.action = "new instance";
    request.address = address;
    request.browserId = browserId;
    request.mobile = isMobileDevice();
    sendRequest(request, function (data) {
        clientCreationInProgress = false;
        if (!data) {
            recordRequestFailure();
            retryClientCreation(address, request);
            return;
        }
        let json;
        try {
            json = JSON.parse(data);
        } catch (error) {
            recordRequestFailure();
            retryClientCreation(address, request);
            return;
        }
        if (clientDisposed) {
            if (typeof json.id === "string") {
                sendRequest({ action: "kill", client: json.id });
            }
            return;
        }
        if (responseHasClientError(json)) {
            return;
        }
        if (typeof json.id !== "string" || typeof json.serverId !== "string") {
            recordRequestFailure();
            retryClientCreation(address, request);
            return;
        }
        recordRequestSuccess();
        clientId = json.id;
        serverId = json.serverId;
        log("Client created, id: " + clientId + ".");
        mainCycleTask = setInterval(mainCycle, period);
        mainCycle();
    });
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

// One synchronization request carries both pending browser events and the update checkpoint.
function sendSynchronizeRequest(callback) {
    if (clientDisposed || clientFailed || clientId == null) {
        return;
    }
    if (callback) {
        synchronizationCallbacks.push(callback);
    }
    if (synchronizationInFlight) {
        return;
    }
    synchronizationInFlight = true;
    const callbacks = synchronizationCallbacks.splice(0);
    callback = function (accepted) {
        synchronizationInFlight = false;
        if (clientDisposed) {
            return;
        }
        for (const done of callbacks) {
            done(accepted);
        }
        if (synchronizationCallbacks.length > 0 || (accepted && events.length > 0)) {
            sendSynchronizeRequest();
        }
    };
    sendRequest(
        {
            action: "synchronize",
            client: clientId,
            events: events,
            lastUpdate: "#" + lastProcessedUpdateId
        },
        function (data) {
            if (clientDisposed) {
                return;
            }
            if (!data) {
                log("Network error.");
                recordRequestFailure();
                if (callback) {
                    callback(false);
                }
                return;
            }
            let json;
            try {
                json = JSON.parse(data);
            } catch (error) {
                recordRequestFailure();
                if (callback) {
                    callback(false);
                }
                return;
            }
            if (responseHasClientError(json)) {
                if (callback) {
                    callback(false);
                }
                return;
            }
            recordRequestSuccess();
            if (!serverStateIsCurrent(json)) {
                if (callback) {
                    callback(false);
                }
                return;
            }
            try {
                processUpdates(json.updates);
                removeProcessedEvents(json.lastEvent);
                reconcileTextInputs();
                if (pageContext) {
                    pageContext.ready();
                }
            } catch (error) {
                showClientError(error);
                if (callback) {
                    callback(false);
                }
                return;
            }
            if (callback) {
                callback(json.result === true);
            }
        },
        "post"
    );
}

function mainCycle() {
    if (pageContext) {
        const overlay = pageContext.root.querySelector(
            "#" + clientErrorOverlayId + ", #" + connectionOverlayId
        );
        if (overlay) {
            pageContext.failure(overlay);
        }
    }
    if (clientFailed) {
        return;
    }
    sendSynchronizeRequest();
}

if (!pageContext) {
    window.addEventListener("error", event => showClientError(event.error));
    window.addEventListener("unhandledrejection", event => showClientError(event.reason));
}

// Disposed runtimes cannot send events, retry uploads or apply late responses.
function disposeClient() {
    clientDisposed = true;
    clearInterval(mainCycleTask);
    if (clientId != null) {
        sendRequest({ action: "kill", client: clientId });
    }
}

function clearPageCache() {
    if (pageContext) {
        pageContext.clearCache();
    }
    return true;
}

function reset() {
    log("The server initiated the client reset.");
    if (pageContext) {
        pageContext.clearCache();
        pageContext.expire();
        return true;
    }
    clientId = null;
    clearInterval(mainCycleTask);
    document.body.innerHTML = "";
    startClient();
    return true;
}

function goToPage(data) {
    const href = data.href;
    if (typeof href == "string") {
        log("The server initiated a switch to another page: '" + href + "'.");
        if (pageContext) {
            pageContext.navigate(href);
        } else {
            window.location.href = href;
        }
    }
    return true;
}

function openPageInNewTab(data) {
    const href = data.href;
    if (typeof href == "string") {
        log("The server opened a new tab: '" + href + "'.");
        window.open(href, "_blank", "noopener");
    }
    return true;
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
    "set options": setOptions,
    "set option": setOption,
    "set carousel sources": setCarouselSources,
    "set carousel source": setCarouselSource,
    "set selected index": setSelectedIndex,
    "set child order": setChildOrder,
    "set max scale": setMaxScale,
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
