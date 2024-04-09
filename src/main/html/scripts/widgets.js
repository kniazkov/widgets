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
        var widget = document.createElement("button");
        handleClickEvent(widget);
        return widget;
    }
};

var createWidget = function(data) {
    var ctor = widgetsLibrary[data.type];
    var id = data.widget;
    if (ctor && id) {
        var widget = ctor();
        widget._id = id;
        widgets[id] = widget;
        console.log("Widget '" + data.type + "' created, id: " + id + '.');
        return true;
    }
    return false;
};

var setChildWidget = function(data) {
    var widget = widgets[data.widget];
    var child = widgets[data.child];
    if (widget && child) {
        widget.innerHTML = "";
        widget.appendChild(child);
        console.log("Widget " + data.child + " is set as a child of widget " + data.widget + '.');
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

var handleClickEvent = function(widget) {
    addEvent(widget, "click", function() {
        sendEventToServer(widget, "click");
    });
};
