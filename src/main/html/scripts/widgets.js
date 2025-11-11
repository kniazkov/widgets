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
        initPointerEvents(widget);
        initFocusEvents(widget);
        return widget;
    },
    "button" : function() {
        var widget = document.createElement("button");
        initPointerEvents(widget);
        return widget;
    }
};

var refreshWidget = function(widget) {
    var states = widget._states;
    var properties = widget._properties;
    var set = { ...properties.normal };
    if (states.disabled) {
        Object.assign(set, properties.invalid, properties.disabled);
    } else if (states.invalid) {
        Object.assign(set, properties.hovered, properties.active, properties.invalid);
    } else if (states.active) {
        Object.assign(set, properties.hovered, properties.active);
    } else if (states.hovered) {
        Object.assign(set, properties.hovered);
    }
    Object.assign(widget.style, set);
};

var createWidget = function(data) {
    var ctor = widgetsLibrary[data.type];
    var id = data.widget;
    if (!ctor || !id) {
        return false;
    }
    var widget = ctor();
    widget._id = id;
    widget._events = {};
    widget._properties = {
        normal: {},
        hovered: {},
        active: {},
        invalid: {},
        disabled: {}
    };
    widget._states = {
        hovered : false,
        active : false,
        invalid : false,
        disabled : false
    };
    widgets[id] = widget;
    log("Widget '" + data.type + "' created, id: " + id + '.');
    return true;
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
    var rgb = data["color"];
    var state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        var color = "rgb(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
        widget._properties[state].color = color;
        refreshWidget(widget);
        log("The color \"" + color + "\" for state \"" + state + "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setBgColor = function(data) {
    var widget = widgets[data.widget];
    var rgb = data["bg color"];
    var state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        var color = "rgb(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
        widget._properties[state].backgroundColor = color;
        refreshWidget(widget);
        log("The background color \"" + color + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
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

var setMargin = function(data) {
    var widget = widgets[data.widget];
    var obj = data.margin;
    if (widget && typeof obj == "object") {
        widget.style.marginLeft = obj.left;
        widget.style.marginRight = obj.right;
        widget.style.marginTop = obj.top;
        widget.style.marginBottom = obj.bottom;
        log("The margin of the widget " + data.widget + " has been set to \"" + JSON.stringify(obj) + "\"");
        return true;
    }
    return false;
};

var setFontFace = function(data) {
    var widget = widgets[data.widget];
    var value = data["font face"];
    var state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        if (value == "default") {
            value = DEFAULT_FONT_FACE;
        }
        widget._properties[state].fontFamily = value;
        refreshWidget(widget);
        log("The font face \"" + value + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontSize = function(data) {
    var widget = widgets[data.widget];
    var value = data["font size"];
    var state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        widget._properties[state].fontSize = value;
        refreshWidget(widget);
        log("The font size \"" + value + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setFontWeight = function(data) {
    var widget = widgets[data.widget];
    var value = data["font weight"];
    var state = data.state;
    if (widget && typeof value == "number" && typeof state == "string") {
        widget._properties[state].fontWeight = value;
        refreshWidget(widget);
        log("The font weight \"" + value + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setItalic = function(data) {
    var widget = widgets[data.widget];
    var value = data["italic"];
    var state = data.state;
    if (widget && typeof value == "boolean" && typeof state == "string") {
        widget._properties[state].fontStyle = value ? "italic" : "normal";
        refreshWidget(widget);
        log("The italic flag \"" + value + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setBorderColor = function(data) {
    var widget = widgets[data.widget];
    var rgb = data["border color"];
    var state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        var color = "rgb(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
        widget._properties[state].borderColor = color;
        refreshWidget(widget);
        log("The border color \"" + color + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setBorderStyle = function(data) {
    var widget = widgets[data.widget];
    var value = data["border style"];
    if (widget && typeof value == "string") {
        widget.style.borderStyle = value;
        log("The border style of the widget " + data.widget + " has been set to \"" + value + "\"");
        return true;
    }
    return false;
};

var setBorderWidth = function(data) {
    var widget = widgets[data.widget];
    var value = data["border width"];
    if (widget && typeof value == "string") {
        widget.style.borderWidth = value;
        log("The border width of the widget " + data.widget + " has been set to \"" + value + "\"");
        return true;
    }
    return false;
};

var setBorderRadius = function(data) {
    var widget = widgets[data.widget];
    var value = data["border radius"];
    if (widget && typeof value == "string") {
        widget.style.borderRadius = value;
        log("The border radius of the widget " + data.widget + " has been set to \"" + value + "\"");
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

var initPointerEvents = function(widget) {
    addEvent(widget, "click", function(event) {
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerenter", function(event) {
        widget._states.hovered = true;
        refreshWidget(widget);
        sendEventToServer(widget, "pointer enter", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerleave", function(event) {
        widget._states.hovered = false;
        refreshWidget(widget);
        sendEventToServer(widget, "pointer leave", processPointerEvent(widget, event));
    });
};

var initFocusEvents = function(widget) {
    addEvent(widget, "focus", function(event) {
        widget._states.active = true;
        refreshWidget(widget);
    });
    addEvent(widget, "blur", function(event) {
        widget._states.active = false;
        refreshWidget(widget);
    });
};
