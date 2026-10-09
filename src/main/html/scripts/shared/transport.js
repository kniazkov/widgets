/*
 * Copyright (c) 2026 Ivan Kniazkov
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

// Object-valued fields are serialized as JSON; POST and file requests use multipart form data.
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
            if (req.status >= 500 && req.status <= 599) {
                complete(
                    JSON.stringify({ result: false, clientError: true, httpStatus: req.status })
                );
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
