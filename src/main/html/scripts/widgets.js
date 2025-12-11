/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

var widgets = { };
var lastFileId = 0;

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
    "active text" : function() {
        var widget = document.createElement("span");
        initPointerEvents(widget, true);
        return widget;
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
    "text area" : function() {
        var widget = document.createElement("textarea");
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
        initPointerEvents(widget, true);
        return widget;
    },
    "file loader" : function() {
        var widget = document.createElement("button");
        widget._files = [];
        widget._multiple = false;
        widget.onClick = function() {
            var input = document.createElement("input");
            input.type = "file";
            input.style.display = "none";
            input.multiple = widget._multiple;
            document.body.appendChild(input);
            addEvent(input, "change", function(evt) {
                var files = evt.target.files;
                if (!files) return;
                for (var index = 0; index < files.length; index++) {
                    loadFile(widget, files[index]);
                }
                document.body.removeChild(input);
            });
            input.click();
        };
        initPointerEvents(widget, true);
        return widget;
    },
    "image" : function() {
        return document.createElement("img");
    },
    "cell" : function() {
        var widget = document.createElement("td");
        initPointerEvents(widget, true);
        return widget;
    },
    "row" : function() {
        var widget = document.createElement("tr");
        initPointerEvents(widget, true);
        return widget;
    },
    "table" : function() {
        var widget = document.createElement("table");
        widget.style.borderCollapse = "separate";
        return widget;
    },
    "inline block" : function() {
        var widget = document.createElement("div");
        widget.style.display = "inline-block";
        initPointerEvents(widget, true);
        return widget;
    },
    "margin decorator" : function() {
        return document.createElement("span");
    },
    "checkbox": function() {
        var widget = document.createElement("input");
        widget.type = "checkbox";
        addEvent(widget, "change", function() {
            sendEventToServer(widget, "check", { state: widget.checked });
        });
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

var setValidFlag = function(data) {
    var widget = widgets[data.widget];
    var flag = data.valid;
    if (widget && typeof flag == "boolean") {
        widget._states.invalid = !flag;
        log("The widget " + data.widget + " has been marked as " +
                (flag ? "valid" : "invalid") + '.');
        refreshWidget(widget);
        return true;
    }
    return false;
};

var setDisabledFlag = function(data) {
    var widget = widgets[data.widget];
    var flag = data.disabled;
    if (widget && typeof flag == "boolean") {
        widget._states.disabled = flag;
        log("The widget " + data.widget + " has been marked as " +
                (flag ? "disabled" : "enabled") + '.');
        refreshWidget(widget);
        widget.disabled = flag;
        return true;
    }
    return false;
};

var setHiddenFlag = function(data) {
    var widget = widgets[data.widget];
    var flag = data.hidden;
    if (widget && typeof flag == "boolean") {
        widget.style.display = flag ? "none" : "";
        log("The widget " + data.widget + " is" + (flag ? "" : " not") + " hidden.");
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
        var color = composeColor(rgb);
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
        var color = composeColor(rgb);
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
        log("The width of the widget " + data.widget + " has been set to \"" + value + "\".");
        return true;
    }
    return false;
};

var setHeight = function(data) {
    var widget = widgets[data.widget];
    var value = data.height;
    if (widget && typeof value == "string") {
        widget.style.height = value;
        log("The height of the widget " + data.widget + " has been set to \"" + value + "\".");
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
        log("The margin of the widget " + data.widget + " has been set to \"" + JSON.stringify(obj) + "\".");
        return true;
    }
    return false;
};

var setPadding = function(data) {
    var widget = widgets[data.widget];
    var obj = data.padding;
    if (widget && typeof obj == "object") {
        widget.style.paddingLeft = obj.left;
        widget.style.paddingRight = obj.right;
        widget.style.paddingTop = obj.top;
        widget.style.paddingBottom = obj.bottom;
        log("The padding of the widget " + data.widget + " has been set to \"" + JSON.stringify(obj) + "\".");
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
        var color = composeColor(rgb);
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
    var style = data["border style"];
    var state = data.state;
    if (widget && typeof style == "string" && typeof state == "string") {
        widget._properties[state].borderStyle = style;
        log("The border style \"" + style + "\" for state \"" + state +
                "\" has been set to the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var setBorderWidth = function(data) {
    var widget = widgets[data.widget];
    var value = data["border width"];
    if (widget && typeof value == "string") {
        widget.style.borderWidth = value;
        log("The border width of the widget " + data.widget + " has been set to \"" + value + "\".");
        return true;
    }
    return false;
};

var setBorderRadius = function(data) {
    var widget = widgets[data.widget];
    var value = data["border radius"];
    if (widget && typeof value == "string") {
        widget.style.borderRadius = value;
        log("The border radius of the widget " + data.widget + " has been set to \"" + value + "\".");
        return true;
    }
    return false;
};

var setSource = function(data) {
    var widget = widgets[data.widget];
    var source = data["source"];
    if (widget && typeof source == "string") {
        widget.src = source;
        log("The source \"" + truncate(source, 100) + "\" has been set to widget \"" + data.widget + "\".");
        return true;
    }
    return false;
};

var setHorzAlignment = function(data) {
    var widget = widgets[data.widget];
    var alignment = data["horz alignment"];
    if (widget && typeof alignment == "string") {
        widget.style.textAlign  = alignment;
        log("The horizontal alignment of the widget " + data.widget + " content has been set to \"" + alignment + "\".");
        return true;
    }
    return false;
};

var setVertAlignment = function(data) {
    var widget = widgets[data.widget];
    var alignment = data["vert alignment"];
    if (widget && typeof alignment == "string") {
        widget.style.verticalAlign = alignment;
        log("The vertical alignment of the widget " + data.widget + " content has been set to \"" + alignment + "\".");
        return true;
    }
    return false;
};

var setCellSpacing = function(data) {
    var widget = widgets[data.widget];
    var value = data["cell spacing"];
    if (widget && typeof value == "string") {
        widget.style.borderSpacing = value;
        log("The cell spacing of the widget " + data.widget + " has been set to \"" + value + "\".");
        return true;
    }
    return false;
};

var setCheckedFlag = function(data) {
    var widget = widgets[data.widget];
    var flag = data.checked;
    if (widget && typeof flag == "boolean") {
        widget.checked = flag;
        log("The widget " + data.widget + " has been " + (flag ? "checked" : "unchecked") + '.');
        return true;
    }
    return false;
};

var setMultipleInput = function(data) {
    var widget = widgets[data.widget];
    var flag = data["multiple input"];
    if (widget && typeof flag == "boolean") {
        widget._multiple = flag;
        log("The multiple input flag has been " + ( flag ? "set" : "cleared" ) + " on the widget " + data.widget + '.');
        return true;
    }
    return false;
};

var sendNextChunk = function(data) {
    var widget = widgets[data.widget];
    if (widget) {
        setTimeout(function() {
            sendNextChunkToServer(widget);
        }, 0);
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

var initPointerEvents = function(widget, activeOnPointerDown) {
    addEvent(widget, "click", function(event) {
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
        if (widget.onClick) {
            widget.onClick();
        }
    });
    addEvent(widget, "pointerenter", function(event) {
        widget._states.hovered = true;
        if (widget._events.click) {
            widget.style.cursor = "pointer";
        }
        refreshWidget(widget);
        sendEventToServer(widget, "pointer enter", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerleave", function(event) {
        if (activeOnPointerDown) {
            widget._states.active = false;
        }
        widget._states.hovered = false;
        if (widget._events.click) {
            widget.style.cursor = "default";
        }
        refreshWidget(widget);
        sendEventToServer(widget, "pointer leave", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerdown", function(event) {
        if (activeOnPointerDown) {
            widget._states.active = true;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer down", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerup", function(event) {
        if (activeOnPointerDown) {
            widget._states.active = false;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer up", processPointerEvent(widget, event));
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

var int2hex = '0123456789abcdef';
var arrayBufferToHex = function(buffer) {
    var bytes = new Uint8Array(buffer);
    var result = [];
    var chunk = [];
    var size = 0;
    for (var index = 0; index < bytes.length; index++) {
        var byte = bytes[index];
        chunk.push(int2hex[byte >> 4]);
        chunk.push(int2hex[byte & 0xF]);
        size++;
        if (size == MAX_UPLOAD_CHUNK_SIZE) {
            result.push(chunk.join(""));
            chunk = [];
            size = 0;
        }
    }
    result.push(chunk.join(""));
    return result;
}

var loadFile = function(widget, descr) {
    var reader = new FileReader();
    addEvent(reader, "load", function(evt) {
        var file = {
            id        : ++lastFileId,
            name      : descr.name,
            type      : descr.type,
            size      : descr.size,
            content   : arrayBufferToHex(evt.target.result),
            nextChunk : 0
        };
        widget._files.push(file);
        if (widget._files.length == 1) {
            sendNextChunkToServer(widget);
        } else {
            sendEmptyChunkToServer(widget, file);
        }
    });
    reader.readAsArrayBuffer(descr);
};

var sendNextChunkToServer = function(widget) {
    var files = widget._files;
    if (files.length == 0) {
        log("The widget " + widget._id + " sent all the files that were selected by the user.");
        return;
    }
    var file = files[0];
    var data = {
        fileId      : file.id,
        name        : file.name,
        type        : file.type,
        size        : file.size,
        content     : file.content[file.nextChunk],
        chunkIndex  : file.nextChunk,
        totalChunks : file.content.length
    };
    file.nextChunk++;
    sendEventToServer(widget, "upload", data);
    if (file.content.length == file.nextChunk) {
        files.shift();
    }
};

var sendEmptyChunkToServer = function(widget, file) {
    var data = {
        fileId      : file.id,
        name        : file.name,
        type        : file.type,
        size        : file.size,
        content     : "",
        chunkIndex  : -1,
        totalChunks : file.content.length
    };
    createEvent(widget, "upload", data);
};

var composeColor = function(rgb) {
    if (typeof rgb.a == "number") {
        return "rgba(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' + rgb.a + ')';
    } else {
        return "rgb(" + rgb.r + ',' + rgb.g + ',' + rgb.b + ')';
    }
};
