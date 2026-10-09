/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

const maxConsecutiveRequestFailures = 3;
const connectionOverlayId = "connection-terminated-overlay";
const clientErrorOverlayId = "client-error-overlay";
let consecutiveRequestFailures = 0;
let reloadRequested = false;
let clientFailed = false;

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

if (!pageContext) {
    window.addEventListener("error", event => showClientError(event.error));
    window.addEventListener("unhandledrejection", event => showClientError(event.reason));
}
