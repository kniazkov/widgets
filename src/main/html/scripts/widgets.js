/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

var widgets = { };

var widgetsLibrary = {
    "root" : function() {
        return document.body;
    },
    "section" : function() {
        return document.createElement("div");
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
        handlePointerEvents(widget);
        return widget;
    }
};

var createWidget = function(data) {
    var ctor = widgetsLibrary[data.type];
    var id = data.widget;
    if (ctor && id) {
        var widget = ctor();
        widget._id = id;
        widget._events = {};
        widgets[id] = widget;
        log("Widget '" + data.type + "' created, id: " + id + '.');
        return true;
    }
    return false;
};

var subscribeToEvent = function(data) {
    var widget = widgets[data.widget];
    var event = data.event;
    if (widget && event) {
        log("Server subscribed to the '" + event + "' event of widget " + widget._id + '.');
        widget._events[event] = true;
    }
};

var setChildWidget = function(data) {
    var widget = widgets[data.widget];
    var container = widgets[data.container];
    if (widget && container) {
        container.innerHTML = "";
        container.appendChild(widget);
        log("Widget " + data.widget + " is set as a child of widget " + data.container + '.');
        return true;
    }
    return false;
};

var appendChildWidget = function(data) {
    var widget = widgets[data.widget];
    var container = widgets[data.container];
    if (widget && container) {
        container.appendChild(widget);
        log("Widget " + data.widget + " is added as a child of widget " + data.container + '.');
        return true;
    }
    return false;
};

var removeChildWidget = function(data) {
    var widget = widgets[data.widget];
    var container = widgets[data.container];
    if (widget && container) {
        container.removeChild(widget);
        log("Widget " + data.widget + " is removed from parent widget " + data.container + '.');
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
            log("The text \"" + data.text + "\" has been set to the widget " + data.widget + '.');
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
            log("The color \"" + color + "\" has been set to the widget " + data.widget + '.');
        }
        return true;
    }
    return false;
};

var setBackgroundColor = function(data) {
    var widget = widgets[data.widget];
    var rgb = data["background color"];
    if (widget && typeof rgb == "object") {
        var color = "rgb(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
        var flag = true;
        if (widget.setColor) {
            flag = widget.setBgColor(color);
        } else {
            widget.style.backgroundColor = color;
        }
        if (flag) {
            log("The background color \"" + color + "\" has been set to the widget " + data.widget + '.');
        }
        return true;
    }
    return false;
};

var setWidth = function(data) {
    var widget = widgets[data.widget];
    var value = data.width;
    if (widget && typeof value == "string") {
        widget.style.width = value;
        log("The width of the widget " + data.widget + " has been set to \"" + value + "\"");
        return true;
    }
    return false;
};

var setHeight = function(data) {
    var widget = widgets[data.widget];
    var value = data.height;
    if (widget && typeof value == "string") {
        widget.style.height = value;
        log("The height of the widget " + data.widget + " has been set to \"" + value + "\"");
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
        log("The font face \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontSize = function(data) {
    var widget = widgets[data.widget];
    var value = data["font size"];
    if (widget && typeof value == "string") {
        widget.style.fontSize = value;
        log("The font size \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontWeight = function(data) {
    var widget = widgets[data.widget];
    var value = data["font weight"];
    if (widget && typeof value == "number") {
        widget.style.fontWeight = value;
        log("The font weight \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setItalic = function(data) {
    var widget = widgets[data.widget];
    var value = data["italic"];
    if (widget && typeof value == "boolean") {
        widget.style.fontStyle = value ? "italic" : "normal";
        log("The italic flag \"" + value + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var processPointerEvent = function(element, event) {
    var rect = element.getBoundingClientRect();
    var data = {};
    data.position = {};
    data.position.element = {
        x: Math.round(event.clientX - rect.left),
        y: Math.round(event.clientY - rect.top)
    };
    data.position.client = {
        x: Math.round(event.clientX),
        y: Math.round(event.clientY)
    };
    data.position.page = {
        x: Math.round(event.pageX),
        y: Math.round(event.pageY)
    };
    data.position.screen = {
        x: Math.round(event.screenX),
        y: Math.round(event.screenY)
    };
    data.type = event.pointerType;
    data.primary = event.isPrimary;
    data.buttons = event.buttons;
    data.keys = {
        ctrl: event.ctrlKey,
        alt: event.altKey,
        shift: event.shiftKey,
        meta: event.metaKey
    };
    data.pressure = event.pressure;
    return data;
};

var handlePointerEvents = function(widget) {
    addEvent(widget, "click", function(event) {
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerenter", function(event) {
        sendEventToServer(widget, "pointer enter", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerleave", function(event) {
        sendEventToServer(widget, "pointer leave", processPointerEvent(widget, event));
    });
};
