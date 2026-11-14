/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Each runtime factory owns these variables, its widget registry and upload scheduler.
const pageContext = typeof page === "undefined" ? null : page;
const tabContext = pageContext?.tab || { id: null };
let clientDisposed = false;
let clientId = null;
let serverId = null;
let channelToken = null;
let browserId = null;
const period = 2500;
let mainCycleTask = null;
let clientCreationInProgress = false;

// Always attach the credentials of this runtime, including cleanup of a late creation.
function sendClientRequest(query, callback, method, files, channel) {
    sendRequest(
        query,
        callback,
        method,
        files,
        channel || { token: channelToken, epoch: serverId }
    );
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
                sendClientRequest({
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
    // Query parameters must never select a context, including during retries.
    delete request.tabId;
    if (tabContext.id) {
        request.tabId = tabContext.id;
    }
    sendRequest(
        request,
        function (data) {
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
                    sendClientRequest({ action: "kill", client: json.id }, null, "post", null, {
                        token: json.channelToken,
                        epoch: json.serverId
                    });
                }
                return;
            }
            if (json.tabContextExpired === true) {
                if (pageContext?.resetTab) {
                    pageContext.resetTab();
                } else {
                    tabContext.id = null;
                    retryClientCreation(address, request);
                }
                return;
            }
            if (responseHasClientError(json)) {
                return;
            }
            if (
                typeof json.id !== "string" ||
                typeof json.serverId !== "string" ||
                typeof json.channelToken !== "string"
            ) {
                recordRequestFailure();
                retryClientCreation(address, request);
                return;
            }
            if (typeof json.tabId === "string") {
                if (tabContext.id && tabContext.id !== json.tabId) {
                    // A concurrent initial navigation already established the document's context.
                    sendClientRequest({ action: "kill", client: json.id }, null, "post", null, {
                        token: json.channelToken,
                        epoch: json.serverId
                    });
                    retryClientCreation(address, request);
                    return;
                }
                tabContext.id = json.tabId;
            }
            recordRequestSuccess();
            clientId = json.id;
            serverId = json.serverId;
            channelToken = json.channelToken;
            log("Client created, id: " + clientId + ".");
            mainCycleTask = setInterval(mainCycle, period);
            mainCycle();
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

// Disposed runtimes cannot send events, retry uploads or apply late responses.
function disposeClient() {
    if (clientDisposed) return;
    clientDisposed = true;
    cancelSynchronization();
    if (typeof disposeUploads === "function") disposeUploads();
    disposeCountdowns();
    disposeAllWidgets();
    clearInterval(mainCycleTask);
    if (clientId != null) {
        sendClientRequest({ action: "kill", client: clientId });
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
    channelToken = null;
    clearInterval(mainCycleTask);
    disposeAllWidgets();
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

function downloadFile(data) {
    if (
        typeof data.name !== "string" ||
        !/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/.test(data.name) ||
        typeof data.contentType !== "string" ||
        !/^[a-z0-9][a-z0-9.+-]*\/[a-z0-9][a-z0-9.+-]*$/.test(data.contentType) ||
        typeof data.content !== "string" ||
        data.content.length > 6990508
    ) {
        return false;
    }
    let decoded;
    try {
        decoded = atob(data.content);
    } catch {
        return false;
    }
    if (!decoded.length || decoded.length > 5 * 1024 * 1024) return false;
    const bytes = Uint8Array.from(decoded, char => char.charCodeAt(0));
    const url = URL.createObjectURL(new Blob([bytes], { type: data.contentType }));
    const link = document.createElement("a");
    link.href = url;
    link.download = data.name;
    link.hidden = true;
    document.body.appendChild(link);
    try {
        link.click();
    } finally {
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
    return true;
}
