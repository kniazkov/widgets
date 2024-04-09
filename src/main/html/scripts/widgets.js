/*
 * Copyright (c) 2024 Ivan Kniazkov
 */

var widgets = { };

var widgetsLibrary = {
    "root" : function() {
        return document.body;
    },
    "paragraph" : function() {
        return document.createElement("p");
    },
    "text" : function() {
        return document.createElement("span");
    },
    "input field" : function() {
        return document.createElement("input");
    },
    "button" : function() {
        return document.createElement("button");
    }
};

var createWidget = function(data) {
    var ctor = widgetsLibrary[data.type];
    var id = data.widget;
    if (ctor && id) {
        var widget = ctor();
        widgets[id] = widget;
        console.log("Widget '" + data.type + "' created, id: " + id + '.');
        return true;
    }
    return false;
};

var appendChildWidget = function(data) {
    var widget = widgets[data.widget];
    var child = widgets[data.child];
    if (widget && child) {
        widget.appendChild(child);
        console.log("Widget " + data.child + " is added as a child of widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setText = function(data) {
    var widget = widgets[data.widget];
    if (widget && typeof data.text == "string") {
        widget.innerHTML = escapeHtml(data.text);
        console.log("The text \"" + data.text + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};
