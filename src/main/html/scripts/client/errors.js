/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

const maxConsecutiveRequestFailures = 3;
const connectionOverlayId = "connection-terminated-overlay";
const clientErrorOverlayId = "client-error-overlay";
let consecutiveRequestFailures = 0;
let reloadRequested = false;
let clientFailed = false;
const sessionRefreshKey = "widgetsSessionRefreshAttempted";

function readProtocolResponse(data) {
    try {
        const response = JSON.parse(data);
        if (response && typeof response === "object" && !Array.isArray(response)) return response;
    } catch {
        // A non-protocol HTTP 200 cannot safely acknowledge queued input.
    }
    showClientError(undefined, "protocol");
    return null;
}

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
    overlay.setAttribute("role", "status");
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
function showClientError(error, reason = "application", description = null) {
    if (clientDisposed || clientFailed) {
        return;
    }
    clientFailed = true;
    stopClientRecovery();
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
                sendClientRequest(
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
    overlay.dataset.reason = reason;
    overlay.setAttribute("role", "alert");
    if (description) {
        overlay.classList.add("widgets-update-protocol-error");
        const details = document.createElement("div");
        details.textContent = description;
        overlay.append(details);
    }
    (pageContext ? pageContext.root : document.body || document.documentElement).appendChild(
        overlay
    );
    if (pageContext) {
        pageContext.failure(overlay);
    }
}

function showUpdateProtocolError(error) {
    showClientError(
        error,
        "protocol",
        "Incompatible widget updates. Reload this page to continue."
    );
}

function responseHasClientError(response) {
    if (response?.sessionExpired === true) {
        if (reloadRequested || clientDisposed || clientFailed) return true;
        // Carry one refresh attempt across the document reload, without retaining credentials.
        try {
            if (!window.sessionStorage.getItem(sessionRefreshKey)) {
                window.sessionStorage.setItem(sessionRefreshKey, "1");
                reloadRequested = true;
                clientFailed = true;
                stopClientRecovery();
                window.location.reload();
                return true;
            }
        } catch {
            // If storage is unavailable, do not risk an unbounded reload loop.
        }
        showClientError(undefined, "session");
        return true;
    }
    if (response?.updateProtocolMismatch === true) {
        showUpdateProtocolError();
        return true;
    }
    if (response?.protocolMismatch === true) {
        showClientError(undefined, "protocol");
        return true;
    }
    if (response?.accessDenied === true) {
        showClientError(undefined, "access");
        return true;
    }
    if (response?.clientError === true) {
        showClientError();
        return true;
    }
    return false;
}

function clearSessionRefreshAttempt() {
    try {
        window.sessionStorage.removeItem(sessionRefreshKey);
    } catch {
        // Storage is optional; request authorization is unaffected.
    }
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
    if (typeof response.serverId !== "string" && response.tabContextExpired !== true) {
        showClientError(undefined, "protocol");
        return false;
    }
    if (response.tabContextExpired === true || response.serverId !== serverId) {
        if (pageContext?.resetTab) {
            pageContext.resetTab();
        } else {
            tabContext.id = null;
            reloadCurrentPage();
        }
        return false;
    }
    if (response.clientAlive !== true) {
        reloadCurrentPage();
        return false;
    }
    return true;
}

if (!pageContext) {
    window.addEventListener("error", event => showClientError(event.error));
    window.addEventListener("unhandledrejection", event => showClientError(event.reason));
}
