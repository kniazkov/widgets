/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

let synchronizationInFlight = false;
const synchronizationCallbacks = [];

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
