/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

// Connection state is shared by the classic scripts loaded for one browser tab.
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

// Events stay in this queue until the server acknowledges their monotonically increasing IDs.
const events = [];
let lastEventId = 0;
let lastProcessedUpdateId = 0;

// A persistent transport failure blocks interaction without discarding the current page state.
function showConnectionTerminated() {
    if (clientFailed || document.getElementById(connectionOverlayId)) {
        return;
    }
    const overlay = document.createElement("div");
    overlay.id = connectionOverlayId;
    overlay.textContent = "Connection Terminated";
    (document.body || document.documentElement).appendChild(overlay);
}

function hideConnectionTerminated() {
    const overlay = document.getElementById(connectionOverlayId);
    if (overlay) {
        overlay.remove();
    }
}

// A client failure is fatal for the current page and must not be retried as a network failure.
function showClientError(error) {
    if (clientFailed) {
        return;
    }
    clientFailed = true;
    clearInterval(mainCycleTask);
    hideConnectionTerminated();
    if (error) {
        console.error("Client Error", error);
    }
    const overlay = document.createElement("div");
    overlay.id = clientErrorOverlayId;
    overlay.textContent = "CLIENT ERROR";
    (document.body || document.documentElement).appendChild(overlay);
}

function responseHasClientError(response) {
    if (response && response.clientError === true) {
        showClientError();
        return true;
    }
    return false;
}

function recordRequestFailure() {
    if (clientFailed) {
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
}

// Reloading the current location preserves both the page path and its query parameters.
function reloadCurrentPage() {
    if (reloadRequested) {
        return;
    }
    reloadRequested = true;
    clearInterval(mainCycleTask);
    window.location.reload();
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
    window.addEventListener("beforeunload", function () {
        if (clientId != null) {
            sendRequest({
                action: "kill",
                client: clientId
            });
        }
    });
    startClient(address, data);
}

function startClient(address, data) {
    if (clientFailed) {
        return;
    }
    const request = { ...data };
    request.action = "new instance";
    request.address = address;
    request.browserId = browserId;
    request.mobile = isMobileDevice();
    sendRequest(request, function (data) {
        if (!data) {
            recordRequestFailure();
            return;
        }
        let json;
        try {
            json = JSON.parse(data);
        } catch (error) {
            recordRequestFailure();
            return;
        }
        if (responseHasClientError(json)) {
            return;
        }
        if (typeof json.id !== "string" || typeof json.serverId !== "string") {
            recordRequestFailure();
            return;
        }
        recordRequestSuccess();
        clientId = json.id;
        serverId = json.serverId;
        log("Client created, id: " + clientId + ".");
        mainCycleTask = setInterval(mainCycle, period);
        mainCycle();
    });
    setTimeout(function () {
        if (clientId == null && !clientFailed) {
            startClient(address, data);
        }
    }, 1000);
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
    for (let i = 0; i < updates.length; i++) {
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

// One synchronization request carries both pending browser events and the update checkpoint.
function sendSynchronizeRequest(callback) {
    sendRequest(
        {
            action: "synchronize",
            client: clientId,
            events: events,
            lastUpdate: "#" + lastProcessedUpdateId
        },
        function (data) {
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
    if (clientFailed) {
        return;
    }
    sendSynchronizeRequest();
}

window.addEventListener("error", event => showClientError(event.error));
window.addEventListener("unhandledrejection", event => showClientError(event.reason));

function reset() {
    log("The server initiated the client reset.");
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
        window.location.href = href;
    }
    return true;
}

// These wire names must match the update actions serialized by the Java server.
const actionHandlers = {
    "create widget": createWidget,
    reset: reset,
    "go to page": goToPage,
    subscribe: subscribeToEvent,
    "set child": setChildWidget,
    "append child": appendChildWidget,
    "remove child": removeChildWidget,
    "set valid": setValidFlag,
    "set disabled": setDisabledFlag,
    "set hidden": setHiddenFlag,
    "set text": setText,
    "set color": setColor,
    "set bg color": setBgColor,
    "set opacity": setOpacity,
    "set width": setWidth,
    "set height": setHeight,
    "set margin": setMargin,
    "set padding": setPadding,
    "set font face": setFontFace,
    "set font size": setFontSize,
    "set font weight": setFontWeight,
    "set italic": setItalic,
    "set border color": setBorderColor,
    "set border style": setBorderStyle,
    "set border width": setBorderWidth,
    "set border radius": setBorderRadius,
    "set source": setSource,
    "set sel source": setSelectedSource,
    "set unsel source": setUnselectedSource,
    "set horz alignment": setHorzAlignment,
    "set vert alignment": setVertAlignment,
    "set cell spacing": setCellSpacing,
    "set checked": setCheckedFlag,
    "set multiple input": setMultipleInput,
    "set accepted files": setAcceptedFiles
};

// These events are client-side protocol primitives and do not require an explicit subscription.
const ALWAYS_ALLOWED_EVENTS = ["text input", "check", "upload"];

function sendEventToServer(widget, type, data) {
    if (widget._events[type] || ALWAYS_ALLOWED_EVENTS.includes(type)) {
        createEvent(widget, type, data);
        sendSynchronizeRequest();
    }
}
