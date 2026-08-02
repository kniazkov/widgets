/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

let xmlHttpObject = null;
const server = window.location.protocol + "//" + window.location.host;

// Returns the transport shared by all protocol requests.
function getXmlHttp() {
    if (xmlHttpObject) {
        return xmlHttpObject;
    }
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
function sendRequest(query, callback, method, files) {
    const req = getXmlHttp();
    let form = null;
    const post = method == "post" || files;
    if (post) {
        form = new FormData();
        for (const key in query) {
            let value = query[key];
            if (typeof value == "object") {
                value = JSON.stringify(value);
            }
            form.append(key, value);
        }
        if (files && files.length) {
            for (let i = 0; i < files.length; i++) {
                form.append("file" + (i > 0 ? i + 1 : ""), files[i], files[i].name);
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
    req.onreadystatechange = function () {
        if (req.readyState == 4) {
            if (req.status == 200) {
                if (callback) {
                    callback(req.responseText);
                }
            }
        }
    };
    req.onerror = function () {
        if (callback) {
            callback(null);
        }
    };
    req.send(form);
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
