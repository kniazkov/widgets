/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

// The registry connects server-side widget IDs to their browser DOM nodes.
const widgets = {};
let lastFileId = 0;
const pendingUploads = [];
const activeUploads = [];
let uploadRequestInFlight = false;
let nextUploadIndex = 0;

// Protocol widget type names map to factories that initialize the matching DOM element.
const widgetsLibrary = {
    root: function () {
        return document.body;
    },
    section: function () {
        const widget = document.createElement("div");
        widget.style.display = "flex";
        widget.style.flexWrap = "wrap";
        widget._setHorzAlignment = function (value) {
            switch (value) {
                case "left":
                    widget.style.justifyContent = "flex-start";
                    widget.style.textAlign = "left";
                    break;
                case "center":
                    widget.style.justifyContent = "center";
                    widget.style.textAlign = "center";
                    break;
                case "right":
                    widget.style.justifyContent = "flex-end";
                    widget.style.textAlign = "right";
                    break;
                case "justify":
                    widget.style.justifyContent = "space-between";
                    widget.style.textAlign = "justify";
                    break;
                default:
                    widget.style.justifyContent = "flex-start";
                    widget.style.textAlign = "left";
                    break;
            }
        };
        widget._setVertAlignment = function (value) {
            switch (value) {
                case "top":
                    widget.style.alignItems = "flex-start";
                    break;
                case "middle":
                    widget.style.alignItems = "center";
                    break;
                case "bottom":
                    widget.style.alignItems = "flex-end";
                    break;
                case "baseline":
                    widget.style.alignItems = "baseline";
                    break;
                default:
                    widget.style.alignItems = "center";
                    break;
            }
        };
        return widget;
    },
    panel: function () {
        const widget = document.createElement("div");
        initPointerEvents(widget, true);
        return widget;
    },
    text: function () {
        return document.createElement("span");
    },
    "active text": function () {
        const widget = document.createElement("span");
        initPointerEvents(widget, true);
        return widget;
    },
    "input field": function () {
        return createInputField();
    },
    "password input": function () {
        const widget = createInputField();
        widget.type = "password";
        return widget;
    },
    "text area": function () {
        const widget = document.createElement("textarea");
        widget.setText = function (text) {
            if (widget.value != text) {
                widget.value = text;
                return true;
            }
            return false;
        };
        addEvent(widget, "input", function () {
            sendEventToServer(widget, "text input", { text: widget.value });
        });
        initPointerEvents(widget);
        initFocusEvents(widget, "active");
        return widget;
    },
    button: function () {
        const widget = document.createElement("button");
        initPointerEvents(widget, true);
        initFocusEvents(widget, "hovered");
        return widget;
    },
    "file loader": function () {
        const widget = document.createElement("button");
        widget._multiple = false;
        widget._accept = "";
        widget._onClick = function () {
            const input = document.createElement("input");
            input.type = "file";
            input.style.display = "none";
            input.multiple = widget._multiple;
            input.accept = widget._accept;
            document.body.appendChild(input);
            addEvent(input, "change", function (evt) {
                const files = evt.target.files;
                if (!files) return;
                loadFiles(widget, files);
                document.body.removeChild(input);
            });
            input.click();
        };
        initPointerEvents(widget, true);
        return widget;
    },
    image: function () {
        return document.createElement("img");
    },
    "active image": function () {
        const widget = document.createElement("img");
        widget._sources = {
            normal: "#",
            hovered: "#",
            active: "#"
        };
        widget._refresh = function () {
            const states = widget._states;
            if (states.active) {
                widget.src = widget._sources.active;
            } else if (states.hovered) {
                widget.src = widget._sources.hovered;
            } else {
                widget.src = widget._sources.normal;
            }
            return true; // also refresh properties
        };
        initPointerEvents(widget, true);
        return widget;
    },
    cell: function () {
        const widget = document.createElement("td");
        initPointerEvents(widget, true);
        widget._setVertAlignment = function (value) {
            widget.style.verticalAlign = value == "baseline" ? "middle" : value;
        };
        return widget;
    },
    row: function () {
        const widget = document.createElement("tr");
        initPointerEvents(widget, true);
        return widget;
    },
    table: function () {
        const widget = document.createElement("table");
        widget.style.borderCollapse = "separate";
        return widget;
    },
    "inline block": function () {
        const widget = document.createElement("div");
        widget.style.display = "inline-block";
        initPointerEvents(widget, true);
        return widget;
    },
    "margin decorator": function () {
        return document.createElement("span");
    },
    checkbox: function () {
        const widget = document.createElement("img");
        widget._selected = false;
        widget._selSrc = "#";
        widget._unselSrc = "#";
        widget._refresh = function () {
            const color = getWidgetProperty(widget, "color");
            const bgColor = getWidgetProperty(widget, "backgroundColor");
            if (widget._selected) {
                widget.src = replaceColorsInSvg(widget._selSrc, color, bgColor);
            } else {
                widget.src = replaceColorsInSvg(widget._unselSrc, color, bgColor);
            }
            return false; // don't refresh properties
        };
        initPointerEvents(widget, true);
        widget._onClick = function () {
            if (widget._states.disabled) {
                return;
            }
            widget._selected = !widget._selected;
            widget._refresh();
            sendEventToServer(widget, "check", { state: widget._selected });
        };
        return widget;
    }
};

// State precedence must stay aligned with refreshWidget so custom renderers see the same value.
function getWidgetProperty(widget, name) {
    const states = widget._states;
    const properties = widget._properties;
    let value = properties.normal[name];
    if (states.hovered) {
        value = properties.hovered[name];
    }
    if (states.active) {
        value = properties.active[name];
    }
    if (states.invalid) {
        value = properties.invalid[name];
    }
    if (states.disabled) {
        value = properties.disabled[name];
    }
    return value;
}

function refreshWidget(widget) {
    let flag = true;
    if (widget._refresh) {
        flag = widget._refresh();
    }
    if (flag) {
        const states = widget._states;
        const properties = widget._properties;
        const set = { ...properties.normal };
        if (states.hovered) {
            Object.assign(set, properties.hovered);
        }
        if (states.active) {
            Object.assign(set, properties.active);
        }
        if (states.invalid) {
            Object.assign(set, properties.invalid);
        }
        if (states.disabled) {
            Object.assign(set, properties.disabled);
        }
        Object.assign(widget.style, set);
    }
}

// Widget metadata uses underscored fields to keep protocol state separate from native DOM fields.
function createWidget(data) {
    const ctor = widgetsLibrary[data.type];
    const id = data.widget;
    if (!ctor || !id) {
        return false;
    }
    const widget = ctor();
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
        hovered: false,
        active: false,
        invalid: false,
        disabled: false
    };
    widget._display = widget.style.display;
    widgets[id] = widget;
    if (widget._refresh) {
        widget._refresh();
    }
    log("Widget '" + data.type + "' created, id: " + id + ".");
    return true;
}

function subscribeToEvent(data) {
    const widget = widgets[data.widget];
    const event = data.event;
    if (widget && event) {
        log("Server subscribed to the '" + event + "' event of widget " + widget._id + ".");
        widget._events[event] = true;
    }
}

function setChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (widget && container) {
        container.innerHTML = "";
        container.appendChild(widget);
        log("Widget " + data.widget + " is set as a child of widget " + data.container + ".");
        return true;
    }
    return false;
}

function appendChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (widget && container) {
        container.appendChild(widget);
        log("Widget " + data.widget + " is added as a child of widget " + data.container + ".");
        return true;
    }
    return false;
}

function removeChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (widget && container) {
        container.removeChild(widget);
        log("Widget " + data.widget + " is removed from parent widget " + data.container + ".");
        return true;
    }
    return false;
}

function setValidFlag(data) {
    const widget = widgets[data.widget];
    const flag = data.valid;
    if (widget && typeof flag == "boolean") {
        widget._states.invalid = !flag;
        log(
            "The widget " +
                data.widget +
                " has been marked as " +
                (flag ? "valid" : "invalid") +
                "."
        );
        refreshWidget(widget);
        return true;
    }
    return false;
}

function setDisabledFlag(data) {
    const widget = widgets[data.widget];
    const flag = data.disabled;
    if (widget && typeof flag == "boolean") {
        widget._states.disabled = flag;
        log(
            "The widget " +
                data.widget +
                " has been marked as " +
                (flag ? "disabled" : "enabled") +
                "."
        );
        refreshWidget(widget);
        widget.disabled = flag;
        return true;
    }
    return false;
}

function setHiddenFlag(data) {
    const widget = widgets[data.widget];
    const flag = data.hidden;
    if (widget && typeof flag == "boolean") {
        widget.style.display = flag ? "none" : widget._display;
        log("The widget " + data.widget + " is" + (flag ? "" : " not") + " hidden.");
        return true;
    }
    return false;
}

function setText(data) {
    const widget = widgets[data.widget];
    if (widget && typeof data.text == "string") {
        let flag = true;
        if (widget.setText) {
            flag = widget.setText(data.text);
        } else {
            widget.innerHTML = escapeHtml(data.text);
        }
        if (flag) {
            log('The text "' + data.text + '" has been set to the widget ' + data.widget + ".");
        }
        return true;
    }
    return false;
}

function setColor(data) {
    const widget = widgets[data.widget];
    const rgb = data["color"];
    const state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        const color = composeColor(rgb);
        widget._properties[state].color = color;
        refreshWidget(widget);
        log(
            'The color "' +
                color +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setBgColor(data) {
    const widget = widgets[data.widget];
    const rgb = data["bg color"];
    const state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        const color = composeColor(rgb);
        widget._properties[state].backgroundColor = color;
        refreshWidget(widget);
        log(
            'The background color "' +
                color +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setOpacity(data) {
    const widget = widgets[data.widget];
    let opacity = data["opacity"];
    const state = data.state;
    if (widget && typeof opacity == "number" && typeof state == "string") {
        if (opacity < 0) {
            opacity = 0;
        } else if (opacity > 1) {
            opacity = 1;
        }
        widget._properties[state].opacity = opacity;
        refreshWidget(widget);
        log(
            'The opacity "' +
                opacity +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setWidth(data) {
    const widget = widgets[data.widget];
    const value = data.width;
    if (widget && typeof value == "string") {
        widget.style.width = value;
        log("The width of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setHeight(data) {
    const widget = widgets[data.widget];
    const value = data.height;
    if (widget && typeof value == "string") {
        widget.style.height = value;
        log("The height of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setMargin(data) {
    const widget = widgets[data.widget];
    const obj = data.margin;
    if (widget && typeof obj == "object") {
        widget.style.marginLeft = obj.left;
        widget.style.marginRight = obj.right;
        widget.style.marginTop = obj.top;
        widget.style.marginBottom = obj.bottom;
        log(
            "The margin of the widget " +
                data.widget +
                ' has been set to "' +
                JSON.stringify(obj) +
                '".'
        );
        return true;
    }
    return false;
}

function setPadding(data) {
    const widget = widgets[data.widget];
    const obj = data.padding;
    if (widget && typeof obj == "object") {
        widget.style.paddingLeft = obj.left;
        widget.style.paddingRight = obj.right;
        widget.style.paddingTop = obj.top;
        widget.style.paddingBottom = obj.bottom;
        log(
            "The padding of the widget " +
                data.widget +
                ' has been set to "' +
                JSON.stringify(obj) +
                '".'
        );
        return true;
    }
    return false;
}

function setFontFace(data) {
    const widget = widgets[data.widget];
    let value = data["font face"];
    const state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        if (value == "default") {
            value = DEFAULT_FONT_FACE;
        }
        widget._properties[state].fontFamily = value;
        refreshWidget(widget);
        log(
            'The font face "' +
                value +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setFontSize(data) {
    const widget = widgets[data.widget];
    const value = data["font size"];
    const state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        widget._properties[state].fontSize = value;
        refreshWidget(widget);
        log(
            'The font size "' +
                value +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setFontWeight(data) {
    const widget = widgets[data.widget];
    const value = data["font weight"];
    const state = data.state;
    if (widget && typeof value == "number" && typeof state == "string") {
        widget._properties[state].fontWeight = value;
        refreshWidget(widget);
        log(
            'The font weight "' +
                value +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setItalic(data) {
    const widget = widgets[data.widget];
    const value = data["italic"];
    const state = data.state;
    if (widget && typeof value == "boolean" && typeof state == "string") {
        widget._properties[state].fontStyle = value ? "italic" : "normal";
        refreshWidget(widget);
        log(
            'The italic flag "' +
                value +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setBorderColor(data) {
    const widget = widgets[data.widget];
    const rgb = data["border color"];
    const state = data.state;
    if (widget && typeof rgb == "object" && typeof state == "string") {
        const color = composeColor(rgb);
        widget._properties[state].borderColor = color;
        refreshWidget(widget);
        log(
            'The border color "' +
                color +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setBorderStyle(data) {
    const widget = widgets[data.widget];
    const style = data["border style"];
    const state = data.state;
    if (widget && typeof style == "string" && typeof state == "string") {
        widget._properties[state].borderStyle = style;
        refreshWidget(widget);
        log(
            'The border style "' +
                style +
                '" for state "' +
                state +
                '" has been set to the widget ' +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setBorderWidth(data) {
    const widget = widgets[data.widget];
    const value = data["border width"];
    if (widget && typeof value == "string") {
        widget.style.borderWidth = value;
        log("The border width of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setBorderRadius(data) {
    const widget = widgets[data.widget];
    const value = data["border radius"];
    if (widget && typeof value == "string") {
        widget.style.borderRadius = value;
        log("The border radius of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setSource(data) {
    const widget = widgets[data.widget];
    const source = data["source"];
    if (widget && typeof source == "string") {
        const state = data.state;
        if (typeof state == "string") {
            widget._sources[state] = source;
            refreshWidget(widget);
            log(
                'The source "' +
                    truncate(source, 100) +
                    '" for state "' +
                    state +
                    '" has been set to the widget ' +
                    data.widget +
                    "."
            );
        } else {
            widget.src = source;
            log(
                'The source "' +
                    truncate(source, 100) +
                    '" has been set to widget "' +
                    data.widget +
                    '".'
            );
        }
        return true;
    }
    return false;
}

function setSelectedSource(data) {
    const widget = widgets[data.widget];
    const source = data["sel source"];
    if (widget && typeof source == "string") {
        widget._selSrc = source;
        log(
            'The source "' +
                truncate(source, 100) +
                '" for selected state has been set to widget "' +
                data.widget +
                '".'
        );
        refreshWidget(widget);
        return true;
    }
    return false;
}

function setUnselectedSource(data) {
    const widget = widgets[data.widget];
    const source = data["unsel source"];
    if (widget && typeof source == "string") {
        widget._unselSrc = source;
        log(
            'The source "' +
                truncate(source, 100) +
                '" for unselected state has been set to widget "' +
                data.widget +
                '".'
        );
        refreshWidget(widget);
        return true;
    }
    return false;
}

function setHorzAlignment(data) {
    const widget = widgets[data.widget];
    const alignment = data["horz alignment"];
    if (widget && widget._setHorzAlignment && typeof alignment == "string") {
        widget._setHorzAlignment(alignment);
        log(
            "The horizontal alignment of the widget " +
                data.widget +
                ' content has been set to "' +
                alignment +
                '".'
        );
        return true;
    }
    return false;
}

function setVertAlignment(data) {
    const widget = widgets[data.widget];
    const alignment = data["vert alignment"];
    if (widget && widget._setVertAlignment && typeof alignment == "string") {
        widget._setVertAlignment(alignment);
        log(
            "The vertical alignment of the widget " +
                data.widget +
                ' content has been set to "' +
                alignment +
                '".'
        );
        return true;
    }
    return false;
}

function setCellSpacing(data) {
    const widget = widgets[data.widget];
    const value = data["cell spacing"];
    if (widget && typeof value == "string") {
        widget.style.borderSpacing = value;
        log("The cell spacing of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setCheckedFlag(data) {
    const widget = widgets[data.widget];
    const flag = data.checked;
    if (widget && typeof flag == "boolean") {
        widget._selected = flag;
        refreshWidget(widget);
        log("The widget " + data.widget + " has been " + (flag ? "checked" : "unchecked") + ".");
        return true;
    }
    return false;
}

function setMultipleInput(data) {
    const widget = widgets[data.widget];
    const flag = data["multiple input"];
    if (widget && typeof flag == "boolean") {
        widget._multiple = flag;
        log(
            "The multiple input flag has been " +
                (flag ? "set" : "cleared") +
                " on the widget " +
                data.widget +
                "."
        );
        return true;
    }
    return false;
}

function setAcceptedFiles(data) {
    const widget = widgets[data.widget];
    const files = data["accepted files"];
    if (widget && typeof files == "string") {
        widget._accept = files;
        if (files == "") {
            log("The widget " + data.widget + " can accept all files");
        } else {
            log("The widget " + data.widget + " can accept files: '" + files + "'.");
        }
        return true;
    }
    return false;
}

// Browser event handling and serialization.
function createInputField() {
    const widget = document.createElement("input");
    widget.setText = function (text) {
        if (widget.value != text) {
            widget.value = text;
            return true;
        }
        return false;
    };
    addEvent(widget, "input", function () {
        sendEventToServer(widget, "text input", { text: widget.value });
    });
    initPointerEvents(widget);
    initFocusEvents(widget, "active");
    return widget;
}

function processPointerEvent(element, event) {
    const rect = element.getBoundingClientRect();
    const data = {};
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
}

function initPointerEvents(widget, activeOnPointerDown) {
    addEvent(widget, "click", function (event) {
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
        if (widget._onClick) {
            widget._onClick();
        }
    });
    addEvent(widget, "pointerenter", function (event) {
        widget._states.hovered = true;
        if (widget._events.click) {
            widget.style.cursor = "pointer";
        }
        refreshWidget(widget);
        sendEventToServer(widget, "pointer enter", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerleave", function (event) {
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
    addEvent(widget, "pointerdown", function (event) {
        if (activeOnPointerDown) {
            widget._states.active = true;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer down", processPointerEvent(widget, event));
    });
    addEvent(widget, "pointerup", function (event) {
        if (activeOnPointerDown) {
            widget._states.active = false;
            refreshWidget(widget);
        }
        sendEventToServer(widget, "pointer up", processPointerEvent(widget, event));
    });
}

function initFocusEvents(widget, state) {
    addEvent(widget, "focus", function (event) {
        widget._states[state] = true;
        refreshWidget(widget);
    });
    addEvent(widget, "blur", function (event) {
        widget._states[state] = false;
        refreshWidget(widget);
    });
}

// Registers all selected files before the binary scheduler starts sending their chunks.
function loadFiles(widget, descriptions) {
    const selected = [];
    for (let index = 0; index < descriptions.length; index++) {
        const descr = descriptions[index];
        if (
            !Number.isSafeInteger(descr.size) ||
            descr.size < 0 ||
            descr.size > uploadProtocol.maxFileSize
        ) {
            log("The selected file '" + descr.name + "' is too large to upload.");
            continue;
        }
        const file = {
            id: ++lastFileId,
            name: descr.name,
            type: descr.type,
            size: descr.size,
            source: descr,
            totalChunks: Math.max(1, Math.ceil(descr.size / uploadProtocol.chunkSize)),
            nextChunk: 0,
            ready: false,
            widget
        };
        pendingUploads.push(file);
        selected.push(file);
        createEvent(widget, "upload", {
            fileId: file.id,
            name: file.name,
            type: file.type,
            size: file.size,
            totalChunks: file.totalChunks
        });
    }
    if (selected.length > 0) {
        acknowledgeSelections(selected);
    }
}

// Waits for the reliable event stream to register descriptors before sending binary data.
function acknowledgeSelections(selected) {
    sendSynchronizeRequest(function (accepted) {
        if (!accepted) {
            setTimeout(function () {
                acknowledgeSelections(selected);
            }, UPLOAD_RETRY_DELAY);
            return;
        }
        for (let index = 0; index < selected.length; index++) {
            selected[index].ready = true;
        }
        fillActiveUploads();
        sendNextUploadChunk();
    });
}

// Moves queued files into the five page-wide active upload slots in selection order.
function fillActiveUploads() {
    while (
        activeUploads.length < MAX_ACTIVE_UPLOADS &&
        pendingUploads.length > 0 &&
        pendingUploads[0].ready
    ) {
        activeUploads.push(pendingUploads.shift());
    }
}

// Removes a completed or rejected upload without skipping the next round-robin entry.
function removeActiveUpload(file) {
    const index = activeUploads.indexOf(file);
    if (index < 0) {
        return;
    }
    activeUploads.splice(index, 1);
    if (index < nextUploadIndex) {
        nextUploadIndex--;
    }
    if (nextUploadIndex >= activeUploads.length) {
        nextUploadIndex = 0;
    }
    fillActiveUploads();
}

// Sends one binary slice, then gives the next active file a turn.
function sendNextUploadChunk() {
    if (uploadRequestInFlight) {
        return;
    }
    fillActiveUploads();
    if (activeUploads.length == 0) {
        return;
    }
    if (nextUploadIndex >= activeUploads.length) {
        nextUploadIndex = 0;
    }
    const file = activeUploads[nextUploadIndex];
    nextUploadIndex = (nextUploadIndex + 1) % activeUploads.length;
    const offset = file.nextChunk * uploadProtocol.chunkSize;
    const chunk = file.source.slice(offset, Math.min(file.size, offset + uploadProtocol.chunkSize));
    uploadRequestInFlight = true;
    sendBinaryRequest(
        {
            action: "upload chunk",
            client: clientId,
            widget: file.widget._id,
            fileId: file.id,
            chunkIndex: file.nextChunk,
            lastUpdate: "#" + lastProcessedUpdateId
        },
        function (data) {
            uploadRequestInFlight = false;
            if (!data) {
                recordRequestFailure();
                setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
                return;
            }
            let receipt;
            try {
                receipt = JSON.parse(data);
            } catch (error) {
                recordRequestFailure();
                setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
                return;
            }
            recordRequestSuccess();
            processUpdates(receipt.updates);
            if (
                receipt.result === true &&
                Number.isInteger(receipt.nextChunk) &&
                receipt.nextChunk >= 0 &&
                receipt.nextChunk <= file.totalChunks &&
                typeof receipt.complete == "boolean" &&
                receipt.complete === (receipt.nextChunk == file.totalChunks)
            ) {
                file.nextChunk = receipt.nextChunk;
                if (receipt.complete === true) {
                    removeActiveUpload(file);
                }
                sendNextUploadChunk();
                return;
            }
            if (receipt.result === false) {
                log("The server rejected upload '" + file.name + "'.");
                removeActiveUpload(file);
                sendNextUploadChunk();
                return;
            }
            setTimeout(sendNextUploadChunk, UPLOAD_RETRY_DELAY);
        },
        chunk
    );
}

// Rendering helpers.
function composeColor(rgb) {
    if (typeof rgb.a == "number") {
        return "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + rgb.a + ")";
    } else {
        return "rgb(" + rgb.r + "," + rgb.g + "," + rgb.b + ")";
    }
}

function replaceColorsInSvg(svg, color, bgColor) {
    if (!color || !bgColor) {
        return svg;
    }
    const prefix = "data:image/svg+xml,";
    const encoded = svg.indexOf(prefix) === 0 ? svg.substring(prefix.length) : svg;
    let decoded = decodeURIComponent(encoded);
    decoded = decoded
        .replace(
            /stroke\s*=\s*(['"])(?:black|#000|#000000|rgb\s*\(\s*0\s*,\s*0\s*,\s*0\s*\))\1/gi,
            'stroke="' + color + '"'
        )
        .replace(
            /fill\s*=\s*(['"])(?:white|#fff|#ffffff|rgb\s*\(\s*255\s*,\s*255\s*,\s*255\s*\))\1/gi,
            'fill="' + bgColor + '"'
        );
    return prefix + encodeURIComponent(decoded);
}
