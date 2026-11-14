/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

const server = window.location.protocol + "//" + window.location.host;
let protocolCsrf = null;
let protocolSession = null;
const widgetsProtocolVersion = "1";

// Keep retries local to the caller that owns the idempotency key or event checkpoint.
function transportRetryDelay(failures, base = 500) {
    const ceiling = 15000;
    const delay = Math.min(ceiling, base * 2 ** Math.min(Math.max(0, failures - 1), 8));
    return Math.min(ceiling, Math.round(delay * (0.75 + Math.random() * 0.5)));
}

// The document bootstrap proves access to this origin; credentials never enter URLs/storage.
function configureProtocolSecurity(csrf, session) {
    protocolCsrf = csrf;
    protocolSession = session;
}

// Creates an independent transport so overlapping protocol requests cannot abort each other.
function getXmlHttp() {
    let xmlHttpObject = null;
    try {
        xmlHttpObject = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e0) {
        try {
            xmlHttpObject = new ActiveXObject("Microsoft.XMLHTTP");
        } catch (e1) {
            xmlHttpObject = false;
        }
    }
    if (!xmlHttpObject && typeof XMLHttpRequest != "undefined") {
        xmlHttpObject = new XMLHttpRequest();
    }
    return xmlHttpObject;
}

// Object-valued fields are serialized as JSON; POST and file requests use multipart form data.
function sendRequest(query, callback, method, files, channel) {
    const req = getXmlHttp();
    const protocolAction = typeof query.action === "string";
    let form = null;
    const hasFiles = files && files.length;
    const post = protocolAction || method == "post" || hasFiles;
    if (post) {
        form = new FormData();
        for (const key in query) {
            let value = query[key];
            if (typeof value == "object") {
                value = JSON.stringify(value);
            }
            form.append(key, value);
        }
        if (hasFiles) {
            for (let i = 0; i < files.length; i++) {
                const entry = files[i];
                const data = entry.data || entry;
                const field = entry.field || "file" + (i > 0 ? i + 1 : "");
                const name = entry.name || data.name || "upload.bin";
                form.append(field, data, name);
            }
        }
        req.open("POST", server, true);
    } else {
        let queryString = "";
        let count = 0;
        for (const key in query) {
            let value = query[key];
            if (typeof value == "object") {
                value = JSON.stringify(value);
            }
            if (count) {
                queryString += "&";
            }
            count++;
            queryString += key + "=" + encodeURIComponent(value);
        }
        req.open("GET", server + "?" + queryString, true);
    }
    if (protocolAction) {
        if (protocolSession) req.setRequestHeader("X-Widgets-Session", protocolSession);
        if (protocolCsrf) req.setRequestHeader("X-Widgets-CSRF", protocolCsrf);
        if (channel) {
            req.setRequestHeader("X-Widgets-Channel", channel.token);
            req.setRequestHeader("X-Widgets-Epoch", channel.epoch);
        }
    }
    let completed = false;
    const complete = function (data) {
        if (completed) {
            return;
        }
        completed = true;
        if (callback) {
            callback(data);
        }
    };
    req.timeout = typeof REQUEST_TIMEOUT == "number" ? REQUEST_TIMEOUT : 10 * 1000;
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status !== 200 && req.status !== 0) {
                console.error(
                    "Widgets HTTP failure",
                    req.status,
                    "requestId=" + (req.getResponseHeader("X-Widgets-Request-Id") || "unavailable")
                );
            }
            if (protocolAction && req.status === 200) {
                const version = req.getResponseHeader("X-Widgets-Protocol-Version");
                complete(
                    version !== null && version !== widgetsProtocolVersion
                        ? JSON.stringify({ protocolMismatch: true })
                        : req.responseText
                );
            } else if (
                protocolAction &&
                req.status === 500 &&
                req.getResponseHeader("X-Widgets-Protocol-Error") === "application"
            ) {
                complete(JSON.stringify({ clientError: true }));
            } else if (protocolAction && req.status === 403) {
                let expired = false;
                try {
                    expired = JSON.parse(req.responseText).sessionExpired === true;
                } catch {
                    // An unrecognized denial is terminal, never an automatic retry loop.
                }
                complete(
                    JSON.stringify(expired ? { sessionExpired: true } : { accessDenied: true })
                );
            } else if (protocolAction && req.status === 401) {
                complete(JSON.stringify({ accessDenied: true }));
            } else if (protocolAction && (req.status === 426 || req.status === 505)) {
                complete(JSON.stringify({ protocolMismatch: true }));
            } else if (
                protocolAction &&
                req.status >= 400 &&
                req.status < 500 &&
                req.status !== 408 &&
                req.status !== 429
            ) {
                complete(JSON.stringify({ clientError: true }));
            } else {
                complete(req.status == 200 ? req.responseText : null);
            }
        }
    };
    req.onerror = function () {
        complete(null);
    };
    req.ontimeout = req.onerror;
    req.onabort = req.onerror;
    req.send(form);
}
