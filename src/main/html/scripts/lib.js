/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

var xmlHttpObject = null;
var server = window.location.protocol + '//' + window.location.host;

var getXmlHttp = function() {
    if (xmlHttpObject) {
        return xmlHttpObject;
    }
    try {
        xmlHttpObject = new ActiveXObject('Msxml2.XMLHTTP');
    }
    catch (e0) {
        try {
            xmlHttpObject = new ActiveXObject('Microsoft.XMLHTTP');
        }
        catch (e1) {
            xmlHttpObject = false;
        }
    }
    if (!xmlHttpObject && typeof XMLHttpRequest!='undefined') {
        xmlHttpObject = new XMLHttpRequest();
    }
    return xmlHttpObject;
};

var sendRequest = function(query, callback, method) {
    var req = getXmlHttp();
    var form = null;
    var post = (method == "post");
    if (post) {
        form = new FormData();
        for (var key in query) {
            var value = query[key];
            if (typeof value == "object") {
                value = JSON.stringify(value);
            }
            form.append(key, value);
        }
        req.open("POST", server, true);
    } else {
        var queryString = "";
        var count = 0;
        for (var key in query) {
            var value = query[key];
            if (typeof value == "object") {
                value = JSON.stringify(value);
            }
            if (count) {
                queryString += '&';
            }
            count++;
            queryString += key + '=' + encodeURIComponent(value);
        }
        req.open("GET", server + '?' + queryString, true);
    }
    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if(req.status == 200) {
                if (callback) {
                    callback(req.responseText);
                }
            }
        }
    };
    req.onerror = function() {
        if (callback) {
            callback(null);
        }
    };
    req.send(form);
};

var addEvent = function(object, type, callback) {
    if (typeof(object) == "string") {
        object = document.getElementById(object);
    }
    if (object == null || typeof(object) == "undefined") {
        return;
    }

    if (object.addEventListener) {
        object.addEventListener(type, callback, false);
    }
    else if (object.attachEvent) {
        object.attachEvent("on" + type, callback);
    }
    else {
        object["on" + type] = callback;
    }
};

var escapeHtml = function(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
};

var log = function(message) {
    console.log(message);
}
