/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

const server = window.location.protocol + "//" + window.location.host;

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

// Serializes protocol fields for a URL query string.
function createQueryString(query) {
    const fields = [];
    for (const key in query) {
        let value = query[key];
        if (typeof value == "object") {
            value = JSON.stringify(value);
        }
        fields.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
    }
    return fields.join("&");
}

// Completes one request exactly once on every XMLHttpRequest terminal path.
function completeRequest(req, body, callback) {
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
            complete(req.status == 200 ? req.responseText : null);
        }
    };
    req.onerror = function () {
        complete(null);
    };
    req.ontimeout = req.onerror;
    req.onabort = req.onerror;
    req.send(body);
}

// Object-valued fields are serialized as JSON; regular POST requests use multipart form data.
function sendRequest(query, callback, method, files) {
    const req = getXmlHttp();
    let form = null;
    const hasFiles = files && files.length;
    const post = method == "post" || hasFiles;
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
        req.open("GET", server + "?" + createQueryString(query), true);
    }
    completeRequest(req, form, callback);
}

// Sends a binary body without a multipart file part that public HTTP filters may reject.
function sendBinaryRequest(query, callback, data) {
    const req = getXmlHttp();
    req.open("POST", server + "?" + createQueryString(query), true);
    req.setRequestHeader("Content-Type", "application/octet-stream");
    completeRequest(req, data, callback);
}

// Registers an event handler through either the standard or legacy DOM API.
function addEvent(object, type, callback) {
    if (typeof object == "string") {
        object = document.getElementById(object);
    }
    if (object == null || typeof object == "undefined") {
        return;
    }
    if (object.addEventListener) {
        object.addEventListener(type, callback, false);
    } else if (object.attachEvent) {
        object.attachEvent("on" + type, callback);
    } else {
        object["on" + type] = callback;
    }
}

function isMobileDevice() {
    const userAgent = navigator.userAgent || navigator.vendor || window.opera;

    const byUserAgent = /iPhone|iPad|iPod|Android|Windows Phone|IEMobile|Opera Mini/i.test(
        userAgent
    );

    const byTouchMac = navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1;

    const byScreen =
        window.matchMedia("(max-width: 768px)").matches &&
        window.matchMedia("(pointer: coarse)").matches;

    return byUserAgent || byTouchMac || byScreen;
}

function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function log(message) {
    console["log"](message);
}

function readBit(number, bitIndex) {
    return (number & (1 << bitIndex)) !== 0;
}

function setBit(number, bitIndex) {
    return number | (1 << bitIndex);
}

function clearBit(number, bitIndex) {
    return number & ~(1 << bitIndex);
}

function truncate(text, maxLength) {
    if (text.length <= maxLength) {
        return text;
    }
    return text.slice(0, maxLength - 3) + "...";
}
