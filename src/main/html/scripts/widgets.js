/*
 * Copyright (c) 2025 Ivan Kniazkov
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
        var widget = document.createElement("input");
        widget.setText = function(text) {
            if (widget.value != text) {
                widget.value = text;
                return true;
            }
            return false;
        }
        addEvent(widget, "input", function() {
            sendEventToServer(widget, "text input", { text : widget.value });
        });
        return widget;
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
        var flag = true;
        if (widget.setText) {
            flag = widget.setText(data.text);
        } else {
            widget.innerHTML = escapeHtml(data.text);
        }
        if (flag) {
            console.log("The text \"" + data.text + "\" has been set to the widget " + data.widget + '.');
        }
        return true;
    }
    return false;
};

var setColor = function(data) {
    var widget = widgets[data.widget];
    if (widget && typeof data.color == "object") {
        var color = "rgb(" + data.color.r + ',' + data.color.g + ',' + data.color.b + ')';
        var flag = true;
        if (widget.setColor) {
            flag = widget.setColor(color);
        } else {
            widget.style.color = color;
        }
        if (flag) {
            console.log("The color \"" + color + "\" has been set to the widget " + data.widget + '.');
        }
        return true;
    }
    return false;
};

var setFontFace = function(data) {
    var widget = widgets[data.widget];
    var value = data["font face"];
    if (widget && typeof value == "string") {
        if (value == "default") {
            value = DEFAULT_FONT_FACE;
        }
        widget.style.fontFamily = value;
        console.log("The font face \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontSize = function(data) {
    var widget = widgets[data.widget];
    var value = data["font size"];
    if (widget && typeof value == "string") {
        widget.style.fontSize = value;
        console.log("The font size \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontWeight = function(data) {
    var widget = widgets[data.widget];
    var value = data["font weight"];
    if (widget && typeof value == "number") {
        widget.style.fontWeight = value;
        console.log("The font weight \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setItalic = function(data) {
    var widget = widgets[data.widget];
    var value = data["italic"];
    if (widget && typeof value == "boolean") {
        widget.style.fontStyle = value ? "italic" : "normal";
        console.log("The italic flag \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var handleClickEvent = function(widget) {
    addEvent(widget, "click", function() {
        sendEventToServer(widget, "click");
    });
};
