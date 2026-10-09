/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Each runtime factory owns these variables, its widget registry and upload scheduler.
const pageContext = typeof page === "undefined" ? null : page;
let clientDisposed = false;
let clientId = null;
let serverId = null;
let browserId = null;
const period = 2500;
let mainCycleTask = null;
let clientCreationInProgress = false;

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
            pageContext.navigate(href, data.replace === true);
        } else if (data.replace === true) {
            window.location.replace(href);
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
