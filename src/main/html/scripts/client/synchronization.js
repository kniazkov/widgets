/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

let synchronizationInFlight = false;
const synchronizationCallbacks = [];
const maxEventStalls = 3;
let eventStalls = 0;
let synchronizationRetryTask = null;
let synchronizationStopped = false;
let synchronizationFailures = 0;

function resumeSynchronizationOnline() {
    if (synchronizationFailures > 0) {
        clearTimeout(synchronizationRetryTask);
        synchronizationRetryTask = null;
    }
    sendSynchronizeRequest();
}

// All entry points (polling, new input and upload callbacks) share the same retry gate.
function scheduleSynchronization(delay) {
    if (
        synchronizationRetryTask !== null ||
        synchronizationStopped ||
        clientDisposed ||
        clientFailed
    )
        return;
    synchronizationRetryTask = setTimeout(() => {
        synchronizationRetryTask = null;
        sendSynchronizeRequest();
    }, delay);
}

function cancelSynchronization() {
    clearTimeout(synchronizationRetryTask);
    synchronizationRetryTask = null;
    synchronizationStopped = true;
    for (const entry of synchronizationCallbacks.splice(0)) entry.done(false);
}

// Rebuild the same URL/tab without replaying actions whose delivery is uncertain.
function resynchronizeEvents(callbacks) {
    cancelSynchronization();
    for (const done of callbacks) done(false);
    reloadCurrentPage();
}

// One synchronization request carries both pending browser events and the update checkpoint.
function sendSynchronizeRequest(callback) {
    if (clientDisposed || clientFailed || synchronizationStopped || clientId == null) {
        return;
    }
    if (callback) {
        if (synchronizationCallbacks.length >= 256) {
            failEventQueue("callbacks");
            callback(false);
            return;
        }
        synchronizationCallbacks.push({ done: callback, through: lastEventId });
    }
    if (synchronizationInFlight || synchronizationRetryTask !== null) {
        return;
    }
    const sentEvents = selectEventBatch();
    if (!sentEvents) return;
    if (navigator.onLine === false) {
        recordRequestFailure();
        scheduleSynchronization(transportRetryDelay(++synchronizationFailures));
        return;
    }
    synchronizationInFlight = true;
    const sentThrough = sentEvents.length ? parseId(sentEvents.at(-1).id) : lastAcknowledgedEventId;
    let stalled = false;
    let transportFailed = false;
    const callbacks = [];
    let moreUpdates = false;
    while (synchronizationCallbacks.length && synchronizationCallbacks[0].through <= sentThrough) {
        callbacks.push(synchronizationCallbacks.shift().done);
    }
    callback = function (accepted) {
        synchronizationInFlight = false;
        if (clientDisposed || synchronizationStopped) {
            return;
        }
        // Install the gate before callbacks: upload metadata callbacks may request another sync.
        if (transportFailed) {
            scheduleSynchronization(transportRetryDelay(++synchronizationFailures));
        } else if (stalled) {
            scheduleSynchronization(250 * 2 ** (eventStalls - 1));
        } else if (!accepted) {
            scheduleSynchronization(period);
        }
        for (const done of callbacks) done(accepted && !stalled);
        if (
            !stalled &&
            accepted &&
            (synchronizationCallbacks.length > 0 || events.length > 0 || moreUpdates)
        ) {
            sendSynchronizeRequest();
        }
    };
    sendClientRequest(
        {
            action: "synchronize",
            client: clientId,
            events: sentEvents,
            lastUpdate: "#" + lastAppliedUpdateSequence
        },
        function (data) {
            if (clientDisposed || synchronizationStopped) {
                return;
            }
            if (data == null) {
                log("Network error.");
                transportFailed = true;
                recordRequestFailure();
                if (callback) {
                    callback(false);
                }
                return;
            }
            const json = readProtocolResponse(data);
            if (!json || responseHasClientError(json)) {
                if (callback) {
                    callback(false);
                }
                return;
            }
            synchronizationFailures = 0;
            if (!serverStateIsCurrent(json)) {
                if (callback) {
                    callback(false);
                }
                return;
            }
            moreUpdates = json.moreUpdates === true;
            if (json.eventBackpressure === true) {
                failEventQueue("server batch");
                for (const done of callbacks) done(false);
                return;
            }
            if (json.eventResyncRequired === true) {
                resynchronizeEvents(callbacks);
                return;
            }
            let removed;
            try {
                // Validate ACKs before applying updates; never discard input created in flight.
                removed =
                    json.result === true ? removeProcessedEvents(json.lastEvent, sentThrough) : 0;
            } catch {
                resynchronizeEvents(callbacks);
                return;
            }
            stalled = sentEvents.length > 0 && removed === 0;
            if (stalled) eventStalls++;
            else if (removed > 0) eventStalls = 0;
            if (eventStalls >= maxEventStalls) {
                resynchronizeEvents(callbacks);
                return;
            }
            try {
                sampleCountdownClock(json.serverNow);
                const activation = sentEvents
                    .slice(0, removed)
                    .findLast(event => event.type === "click");
                if (!processUpdates(json, widgets[activation?.widget])) return;
                reconcileTextInputs();
                recordRequestSuccess();
                if (pageContext && !moreUpdates) {
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
