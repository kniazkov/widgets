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
        return typeof page === "undefined" ? document.body : page.root;
    },
    "text flow": function () {
        const widget = document.createElement("div");
        widget.className = "widgets-text-flow";
        widget.style.overflowWrap = "anywhere";
        widget._setHorzAlignment = value => {
            widget.style.textAlign = value;
        };
        widget._setVertAlignment = value => {
            widget.style.setProperty("--flow-alignment", value);
        };
        return widget;
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
    "sortable section": createSortableSection,
    "zoom decorator": createZoomDecorator,
    panel: function () {
        const widget = document.createElement("div");
        initPointerEvents(widget, true);
        return widget;
    },
    "sticky panel": function () {
        const widget = document.createElement("div");
        widget.style.position = "sticky";
        widget.style.zIndex = "1";
        widget._setStickySide = function (side) {
            widget.style.top = side == "top" ? "0px" : "";
            widget.style.bottom = side == "bottom" ? "0px" : "";
        };
        widget._setStickySide("top");
        initPointerEvents(widget, true);
        return widget;
    },
    popup: function () {
        return createPopup(false);
    },
    "modal popup": function () {
        return createPopup(true);
    },
    text: function () {
        return document.createElement("span");
    },
    "active text": function () {
        const widget = document.createElement("span");
        initPointerEvents(widget, true);
        return widget;
    },
    link: function () {
        const widget = document.createElement("a");
        widget.setAttribute("href", "#");
        initPointerEvents(widget, true);
        initFocusEvents(widget);
        return widget;
    },
    "input field": function () {
        return createInputField();
    },
    "suggestion field": createSuggestionField,
    "password input": function () {
        const widget = createInputField();
        widget.type = "password";
        return widget;
    },
    "text area": function () {
        const widget = document.createElement("textarea");
        initEditableText(widget);
        initPointerEvents(widget);
        initFocusEvents(widget);
        initTextAlignment(widget);
        return widget;
    },
    "drop down list": function () {
        const widget = document.createElement("select");
        widget.style.appearance = "none";
        widget.style.backgroundImage =
            'url("data:image/svg+xml,%3Csvg xmlns=%27http://www.w3.org/2000/svg%27 width=%2712%27 height=%278%27 viewBox=%270 0 12 8%27 fill=%27none%27%3E%3Cpath d=%27M1 1.5L6 6.5L11 1.5%27 stroke=%27%23475569%27 stroke-width=%272%27 stroke-linecap=%27round%27 stroke-linejoin=%27round%27/%3E%3C/svg%3E")';
        widget.style.backgroundPosition = "95% 50%";
        widget.style.backgroundRepeat = "no-repeat";
        widget.style.backgroundSize = "12px 8px";
        widget._rightPaddingOffset = 20;
        addEvent(widget, "change", function () {
            sendEventToServer(widget, "select", { index: widget.selectedIndex });
        });
        initPointerEvents(widget, true);
        initFocusEvents(widget);
        return widget;
    },
    button: function () {
        const widget = document.createElement("button");
        initPointerEvents(widget, true);
        initFocusEvents(widget);
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
        initFocusEvents(widget);
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
    carousel: function () {
        const widget = document.createElement("span");
        const track = document.createElement("span");
        widget.className = "carousel";
        widget.style.display = "inline-block";
        widget.style.overflow = "hidden";
        widget.style.lineHeight = "0";
        widget.style.touchAction = "pan-y";
        track.style.display = "flex";
        track.style.width = "100%";
        track.style.height = "100%";
        track.style.transform = "translateX(0%)";
        track.style.willChange = "transform";
        widget.appendChild(track);
        widget._track = track;
        widget._images = [];
        widget._sources = [];
        widget._selectedIndex = 0;
        widget._showSelectedImage = function () {
            widget._track.style.transform = "translateX(-" + widget._selectedIndex * 100 + "%)";
        };
        initPointerEvents(widget, true);
        initCarouselGestures(widget);
        return widget;
    },
    cell: function () {
        const widget = document.createElement("td");
        initPointerEvents(widget, true);
        widget._setVertAlignment = function (value) {
            widget.style.verticalAlign = value;
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
    "overlay stack": function () {
        const widget = document.createElement("div");
        widget.style.display = "inline-grid";
        widget.style.verticalAlign = "middle";
        widget._layoutChild = function (child) {
            child.style.gridArea = "1 / 1";
        };
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
        widget._usesSvgColors = true;
        widget._refresh = function () {
            const color = getWidgetProperty(widget, "color");
            const bgColor = getWidgetProperty(widget, "backgroundColor");
            if (widget._selected) {
                widget.src = replaceColorsInSvg(widget._selSrc, color, bgColor);
            } else {
                widget.src = replaceColorsInSvg(widget._unselSrc, color, bgColor);
            }
            return true;
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
    },
    "radio button": function () {
        const widget = document.createElement("img");
        widget._selected = false;
        widget._selSrc = "#";
        widget._unselSrc = "#";
        widget._usesSvgColors = true;
        widget._refresh = function () {
            const color = getWidgetProperty(widget, "color");
            const bgColor = getWidgetProperty(widget, "backgroundColor");
            if (widget._selected) {
                widget.src = replaceColorsInSvg(widget._selSrc, color, bgColor);
            } else {
                widget.src = replaceColorsInSvg(widget._unselSrc, color, bgColor);
            }
            return true;
        };
        initPointerEvents(widget, true);
        widget._onClick = function () {
            if (widget._states.disabled || widget._selected) {
                return;
            }
            widget._selected = true;
            widget._refresh();
            sendEventToServer(widget, "check", { state: true });
        };
        return widget;
    }
};

// Popups are fixed to the viewport. A modal popup owns an adjacent full-screen backdrop.
function createPopup(modal) {
    const widget = document.createElement("div");
    widget.className = modal ? "modal-popup" : "popup";
    widget.style.position = "fixed";
    widget.style.zIndex = "1001";
    widget._horzAlignment = "center";
    widget._vertAlignment = "middle";
    widget._refreshPosition = function () {
        let translateX = "0";
        let translateY = "0";
        widget.style.left = "";
        widget.style.right = "";
        widget.style.top = "";
        widget.style.bottom = "";

        switch (widget._horzAlignment) {
            case "left":
                widget.style.left = "0px";
                break;
            case "right":
                widget.style.right = "0px";
                break;
            default:
                widget.style.left = "50%";
                translateX = "-50%";
                break;
        }
        switch (widget._vertAlignment) {
            case "top":
                widget.style.top = "0px";
                break;
            case "bottom":
                widget.style.bottom = "0px";
                break;
            default:
                widget.style.top = "50%";
                translateY = "-50%";
                break;
        }
        widget.style.transform = "translate(" + translateX + ", " + translateY + ")";
    };
    widget._setHorzAlignment = function (value) {
        widget._horzAlignment = value;
        widget._refreshPosition();
    };
    widget._setVertAlignment = function (value) {
        widget._vertAlignment = value;
        widget._refreshPosition();
    };
    widget._refreshPosition();

    if (modal) {
        const backdrop = document.createElement("div");
        backdrop.className = "popup-backdrop";
        backdrop.style.position = "fixed";
        backdrop.style.inset = "0px";
        backdrop.style.zIndex = "1000";
        widget._backdrop = backdrop;
        widget._closeOnOutsideClick = false;
        let pressedOutside = false;
        backdrop.addEventListener("pointerdown", event => {
            pressedOutside =
                event.target === backdrop && event.button === 0 && event.isPrimary !== false;
            event.stopPropagation();
        });
        backdrop.addEventListener("pointercancel", () => {
            pressedOutside = false;
        });
        backdrop.addEventListener("click", event => {
            event.preventDefault();
            event.stopPropagation();
            const activate = pressedOutside || event.detail === 0;
            pressedOutside = false;
            if (
                widget._closeOnOutsideClick &&
                event.target === backdrop &&
                event.button === 0 &&
                activate
            ) {
                sendEventToServer(widget, "dismiss", {});
            }
        });
        widget._onAttached = function () {
            if (widget.parentNode) {
                widget.parentNode.insertBefore(backdrop, widget);
            }
        };
        widget._onDetached = function () {
            backdrop.remove();
        };
    }
    return widget;
}

// State precedence must stay aligned with refreshWidget so custom renderers see the same value.
function getWidgetProperty(widget, name) {
    const states = widget._states;
    const properties = widget._properties;
    let value = properties.normal[name];
    if (states.hovered) {
        value = properties.hovered[name];
    }
    if (states.focused) {
        value = properties.focused[name];
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
        if (states.focused) {
            Object.assign(set, properties.focused);
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
        if (widget._usesSvgColors) {
            delete set.color;
            delete set.backgroundColor;
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
    widget._clientActions = {};
    widget._properties = {
        normal: {},
        hovered: {},
        focused: {},
        active: {},
        invalid: {},
        disabled: {}
    };
    widget._states = {
        hovered: false,
        focused: false,
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

function setClientEventAction(data) {
    const widget = widgets[data.widget];
    const event = data.event;
    const clientAction = data.clientAction;
    if (
        widget &&
        typeof event == "string" &&
        clientAction &&
        typeof clientAction.action == "string"
    ) {
        widget._clientActions[event] = clientAction;
        log("A client action was set for the '" + event + "' event of widget " + widget._id + ".");
        return true;
    }
    return false;
}

function setChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (widget && container) {
        if (container._setChild) {
            container._setChild(widget);
        } else {
            container.innerHTML = "";
            container.appendChild(widget);
        }
        if (widget._onAttached) {
            widget._onAttached();
        }
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
        if (container._layoutChild) {
            container._layoutChild(widget);
        }
        if (widget._onAttached) {
            widget._onAttached();
        }
        log("Widget " + data.widget + " is added as a child of widget " + data.container + ".");
        return true;
    }
    return false;
}

function insertChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    const index = data.index;
    if (
        widget &&
        container &&
        Number.isInteger(index) &&
        index >= 0 &&
        index <= container.children.length
    ) {
        container.insertBefore(widget, container.children[index] || null);
        if (container._layoutChild) {
            container._layoutChild(widget);
        }
        if (widget._onAttached) {
            widget._onAttached();
        }
        log(
            "Widget " +
                data.widget +
                " is inserted at position " +
                index +
                " of widget " +
                data.container +
                "."
        );
        return true;
    }
    return false;
}

function removeChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (widget && container) {
        if (widget._onDetached) {
            widget._onDetached();
        }
        (container._childHost || container).removeChild(widget);
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
        if (flag) widget._closeSuggestions?.();
        return true;
    }
    return false;
}

function setHiddenFlag(data) {
    const widget = widgets[data.widget];
    const flag = data.hidden;
    if (widget && typeof flag == "boolean") {
        if (flag) widget._closeSuggestions?.();
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

function setOptions(data) {
    const widget = widgets[data.widget];
    const options = data.options;
    if (!widget || widget.tagName != "SELECT" || !Array.isArray(options)) {
        return false;
    }
    if (options.some(item => typeof item != "string")) {
        return false;
    }
    const selectedIndex = widget.selectedIndex;
    widget.replaceChildren();
    for (const item of options) {
        const option = document.createElement("option");
        option.textContent = item;
        widget.appendChild(option);
    }
    widget.selectedIndex = selectedIndex;
    log("The options of the widget " + data.widget + " have been replaced.");
    return true;
}

function setOption(data) {
    const widget = widgets[data.widget];
    const index = data.index;
    const text = data.text;
    if (
        widget &&
        widget.tagName == "SELECT" &&
        Number.isInteger(index) &&
        index >= 0 &&
        index < widget.options.length &&
        typeof text == "string"
    ) {
        widget.options[index].textContent = text;
        log("The option " + index + " of the widget " + data.widget + " has been changed.");
        return true;
    }
    return false;
}

function setCarouselSources(data) {
    const widget = widgets[data.widget];
    const sources = data.sources;
    if (
        !widget ||
        !widget._track ||
        !Array.isArray(sources) ||
        sources.length == 0 ||
        sources.some(source => typeof source != "string")
    ) {
        return false;
    }
    widget._sources = sources.slice();
    widget._images = sources.map(source => {
        const image = document.createElement("img");
        image.src = source;
        image.loading = "eager";
        image.decoding = "async";
        image.draggable = false;
        image.style.display = "block";
        image.style.flex = "0 0 100%";
        image.style.width = "100%";
        image.style.height = "100%";
        image.style.objectFit = "contain";
        return image;
    });
    widget._track.replaceChildren(...widget._images);
    if (widget._selectedIndex >= sources.length) {
        widget._selectedIndex = sources.length - 1;
    }
    widget._showSelectedImage();
    log("The image sources of widget " + data.widget + " have been replaced.");
    return true;
}

function setCarouselSource(data) {
    const widget = widgets[data.widget];
    const index = data.index;
    const source = data.source;
    if (
        !widget ||
        !widget._track ||
        !Number.isInteger(index) ||
        index < 0 ||
        index >= widget._sources.length ||
        typeof source != "string"
    ) {
        return false;
    }
    widget._sources[index] = source;
    widget._images[index].src = source;
    log("The image source " + index + " of widget " + data.widget + " has been changed.");
    return true;
}

function setSelectedIndex(data) {
    const widget = widgets[data.widget];
    const index = data["selected index"];
    if (
        widget &&
        widget._track &&
        Number.isInteger(index) &&
        index >= 0 &&
        index < widget._sources.length
    ) {
        widget._selectedIndex = index;
        widget._showSelectedImage();
        log("The index " + index + " has been selected in widget " + data.widget + ".");
        return true;
    }
    if (
        widget &&
        widget.tagName == "SELECT" &&
        Number.isInteger(index) &&
        index >= -1 &&
        index < widget.options.length
    ) {
        widget.selectedIndex = index;
        log("The index " + index + " has been selected in widget " + data.widget + ".");
        return true;
    }
    return false;
}

function setHref(data) {
    const widget = widgets[data.widget];
    const href = data.href;
    if (widget && typeof href == "string") {
        widget.setAttribute("href", href);
        log("The hyperlink of the widget " + data.widget + ' has been set to "' + href + '".');
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

function setBackdropColor(data) {
    const widget = widgets[data.widget];
    const rgb = data["backdrop color"];
    if (widget && widget._backdrop && rgb !== null && typeof rgb == "object") {
        const color = composeColor(rgb);
        widget._backdrop.style.backgroundColor = color;
        log('The backdrop color "' + color + '" has been set to the widget ' + data.widget + ".");
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

function setMaxWidth(data) {
    const widget = widgets[data.widget];
    const value = data["max width"];
    if (widget && typeof value == "string") {
        widget.style.maxWidth = value;
        return true;
    }
    return false;
}

function setMaxHeight(data) {
    const widget = widgets[data.widget];
    const value = data["max height"];
    if (widget && typeof value == "string") {
        widget.style.maxHeight = value;
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
        widget.style.paddingRight = widget._rightPaddingOffset
            ? `calc(${obj.right} + ${widget._rightPaddingOffset}px)`
            : obj.right;
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

function setTextDecoration(data) {
    const widget = widgets[data.widget];
    const value = data["text decoration"];
    const state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        widget._properties[state].textDecoration = value;
        refreshWidget(widget);
        log(
            'The text decoration "' +
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

function setBoxShadow(data) {
    const widget = widgets[data.widget];
    const value = data["box shadow"];
    const state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        widget._properties[state].boxShadow = value;
        refreshWidget(widget);
        log(
            'The box shadow "' +
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

function setOutline(data) {
    const widget = widgets[data.widget];
    const value = data.outline;
    const state = data.state;
    if (
        widget &&
        value !== null &&
        typeof value == "object" &&
        value.color !== null &&
        typeof value.color == "object" &&
        typeof value.style == "string" &&
        typeof value.width == "string" &&
        typeof value.offset == "string" &&
        typeof state == "string"
    ) {
        const properties = widget._properties[state];
        properties.outlineColor = composeColor(value.color);
        properties.outlineStyle = value.style;
        properties.outlineWidth = value.width;
        properties.outlineOffset = value.offset;
        refreshWidget(widget);
        log(
            'The outline for state "' + state + '" has been set to the widget ' + data.widget + "."
        );
        return true;
    }
    return false;
}

function setCursor(data) {
    const widget = widgets[data.widget];
    const value = data.cursor;
    const state = data.state;
    if (widget && typeof value == "string" && typeof state == "string") {
        widget._properties[state].cursor = value;
        refreshWidget(widget);
        log(
            'The cursor "' +
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

function setTransition(data) {
    const widget = widgets[data.widget];
    const value = data.transition;
    if (widget && typeof value == "string") {
        widget.style.transition = value;
        log("The transition of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setBoxSizing(data) {
    const widget = widgets[data.widget];
    const value = data["box sizing"];
    if (widget && typeof value == "string") {
        widget.style.boxSizing = value;
        log("The box sizing of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setOverflow(data) {
    const widget = widgets[data.widget];
    const value = data.overflow;
    if (widget && typeof value == "string") {
        widget.style.overflow = value;
        log("The overflow of the widget " + data.widget + ' has been set to "' + value + '".');
        return true;
    }
    return false;
}

function setIntrinsicSize(data) {
    const widget = widgets[data.widget];
    const value = data["intrinsic size"];
    if (!widget || widget.tagName !== "IMG" || typeof value !== "string") return false;
    if (value === "") {
        widget.removeAttribute("width");
        widget.removeAttribute("height");
        widget.classList.remove("intrinsic-image-size");
        return true;
    }
    if (!/^[1-9][0-9]* [1-9][0-9]*$/.test(value)) return false;
    const [width, height] = value.split(" ").map(Number);
    if (![width, height].every(size => Number.isSafeInteger(size) && size <= 2147483647)) {
        return false;
    }
    widget.setAttribute("width", String(width));
    widget.setAttribute("height", String(height));
    widget.classList.add("intrinsic-image-size");
    return true;
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

function setStickySide(data) {
    const widget = widgets[data.widget];
    const side = data["sticky side"];
    if (widget && widget._setStickySide && (side == "top" || side == "bottom")) {
        widget._setStickySide(side);
        log("The sticky side of the widget " + data.widget + ' has been set to "' + side + '".');
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
    initEditableText(widget);
    initPointerEvents(widget);
    initFocusEvents(widget, "active");
    initTextAlignment(widget);
    return widget;
}

// Suggestions are view state; the text and source list remain server-backed properties.
function createSuggestionField() {
    const widget = createInputField();
    widget.setAttribute("role", "combobox");
    widget.setAttribute("aria-autocomplete", "list");
    widget.setAttribute("aria-expanded", "false");
    widget.autocomplete = "off";
    widget._suggestions = [];
    widget._suggestionSeparator = "";
    let list = null;
    let active = -1;
    let observer = null;
    let composing = false;

    function close() {
        if (!list) return;
        list.remove();
        list = null;
        active = -1;
        widget.setAttribute("aria-expanded", "false");
        widget.removeAttribute("aria-controls");
        widget.removeAttribute("aria-activedescendant");
        document.removeEventListener("pointerdown", outside, true);
        window.removeEventListener("resize", position);
        document.removeEventListener("scroll", position, true);
        window.visualViewport?.removeEventListener("resize", position);
        window.visualViewport?.removeEventListener("scroll", position);
        observer?.disconnect();
        observer = null;
    }

    function outside(event) {
        if (event.target !== widget && !list?.contains(event.target)) close();
    }

    function position() {
        if (!list) return;
        const rect = widget.getBoundingClientRect();
        const viewport = window.visualViewport;
        const top = viewport?.offsetTop || 0;
        const left = viewport?.offsetLeft || 0;
        const height = viewport?.height || window.innerHeight;
        const width = viewport?.width || window.innerWidth;
        const below = Math.max(0, top + height - rect.bottom - 4);
        const above = Math.max(0, rect.top - top - 4);
        const upward = below < 160 && above > below;
        list.style.width = Math.min(rect.width, width - 8) + "px";
        list.style.left =
            Math.max(left + 4, Math.min(rect.left, left + width - rect.width - 4)) + "px";
        list.style.maxHeight = Math.min(240, upward ? above : below) + "px";
        list.style.top = (upward ? rect.top - 4 : rect.bottom + 4) + "px";
        list.style.transform = upward ? "translateY(-100%)" : "";
    }

    function highlight(index) {
        if (!list) return;
        active = index;
        Array.from(list.children).forEach((option, i) => {
            option.setAttribute("aria-selected", String(i === active));
        });
        const option = list.children[active];
        if (option) {
            widget.setAttribute("aria-activedescendant", option.id);
            option.scrollIntoView?.({ block: "nearest" });
        } else {
            widget.removeAttribute("aria-activedescendant");
        }
    }

    // A selection spanning separators is deliberately not replaced by a suggestion.
    function token() {
        const text = widget.value;
        const separator = widget._suggestionSeparator;
        if (!separator) return { start: 0, end: text.length, query: text };
        const from = widget.selectionStart ?? text.length;
        const to = widget.selectionEnd ?? from;
        let start = 0;
        let index = 0;
        while (true) {
            const boundary = text.indexOf(separator, start);
            const end = boundary < 0 ? text.length : boundary;
            if (from <= end) {
                if (to > end) return null;
                const raw = text.slice(start, end);
                const leading = raw.match(/^\s*/)[0].length;
                const trailing = raw.trim() ? raw.match(/\s*$/)[0].length : 0;
                return { start: start + leading, end: end - trailing, query: raw.trim(), index };
            }
            if (boundary < 0 || from < end + separator.length) return null;
            start = end + separator.length;
            index++;
        }
    }

    function keyOf(value) {
        return value.trim().toLowerCase();
    }

    function otherValues(current) {
        return new Set(
            widget._suggestionSeparator
                ? widget.value
                      .split(widget._suggestionSeparator)
                      .filter((value, index) => index !== current.index)
                      .map(keyOf)
                : []
        );
    }

    function removeDuplicates() {
        if (!widget._suggestionSeparator || widget.disabled || composing) return;
        const seen = new Set();
        const value = widget.value
            .split(widget._suggestionSeparator)
            .filter(token => {
                const key = keyOf(token);
                // Empty tokens remain editable; this is not required-value validation.
                if (!key) return true;
                if (seen.has(key)) return false;
                seen.add(key);
                return true;
            })
            .join(widget._suggestionSeparator);
        if (value !== widget.value) {
            widget.value = value;
            widget.dispatchEvent(new Event("input", { bubbles: true }));
        }
    }

    function select(value) {
        if (widget.disabled) return;
        const current = token();
        if (!current || composing || otherValues(current).has(keyOf(value))) return;
        widget.value =
            widget.value.slice(0, current.start) + value + widget.value.slice(current.end);
        const caret = current.start + value.length;
        widget.setSelectionRange(caret, caret);
        close();
        // Use the ordinary text-input pipeline, including delayed-echo protection.
        widget.dispatchEvent(new Event("input", { bubbles: true }));
        close();
    }

    function render() {
        close();
        if (widget.disabled || document.activeElement !== widget || composing) return;
        const current = token();
        if (!current) return;
        const query = current.query.toLowerCase();
        const used = otherValues(current);
        const values = [...new Set(widget._suggestions)].filter(
            value =>
                value.trim() &&
                !used.has(keyOf(value)) &&
                value.toLowerCase().includes(query) &&
                (!widget._suggestionSeparator || !value.includes(widget._suggestionSeparator))
        );
        if (!values.length) return;
        list = document.createElement("div");
        list.className = "suggestion-list";
        list.id = "suggestions-" + widget._id;
        list.setAttribute("role", "listbox");
        const style = getComputedStyle(widget);
        list.style.font = style.font;
        list.style.color = style.color;
        list.style.backgroundColor = style.backgroundColor;
        list.style.borderColor = style.borderColor;
        list.style.borderRadius = style.borderRadius;
        values.forEach((value, index) => {
            const option = document.createElement("div");
            option.id = list.id + "-" + index;
            option.setAttribute("role", "option");
            option.setAttribute("aria-selected", "false");
            option.textContent = value;
            // Keep focus and the on-screen keyboard on the editable field.
            option.addEventListener("pointerdown", event => event.preventDefault());
            option.addEventListener("click", () => select(value));
            list.appendChild(option);
        });
        document.body.appendChild(list);
        // The top layer also works when the field is inside a modal popup.
        if (typeof list.showPopover === "function") {
            list.setAttribute("popover", "manual");
            list.showPopover();
        }
        widget.setAttribute("aria-controls", list.id);
        widget.setAttribute("aria-expanded", "true");
        position();
        document.addEventListener("pointerdown", outside, true);
        window.addEventListener("resize", position);
        document.addEventListener("scroll", position, true);
        window.visualViewport?.addEventListener("resize", position);
        window.visualViewport?.addEventListener("scroll", position);
        // Parent removal does not invoke each descendant's detach hook.
        observer = new MutationObserver(() => {
            if (!widget.isConnected) close();
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    widget._setSuggestions = values => {
        widget._suggestions = values.slice();
        render();
    };
    widget._setSuggestionSeparator = value => {
        widget._suggestionSeparator = value;
        render();
    };
    widget.addEventListener("keyup", event => {
        if (["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) render();
    });
    widget._closeSuggestions = close;
    widget._onDetached = close;
    widget.addEventListener("focus", render);
    widget.addEventListener("click", render);
    widget.addEventListener("input", render);
    widget.addEventListener("blur", () => {
        close();
        removeDuplicates();
    });
    widget.addEventListener("compositionstart", () => {
        composing = true;
        close();
    });
    widget.addEventListener("compositionend", () => {
        composing = false;
        if (document.activeElement !== widget) removeDuplicates();
        render();
    });
    widget.addEventListener("keydown", event => {
        if (event.isComposing || composing) return;
        if (event.key === "Escape" || event.key === "Tab") {
            if (list && event.key === "Escape") {
                event.preventDefault();
                event.stopPropagation();
            }
            close();
        } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            if (!list) render();
            if (list) {
                const count = list.children.length;
                highlight(
                    event.key === "ArrowDown"
                        ? (active + 1) % count
                        : active < 0
                          ? count - 1
                          : (active + count - 1) % count
                );
            }
        } else if (event.key === "Enter" && list && active >= 0) {
            event.preventDefault();
            select(list.children[active].textContent);
        }
    });
    return widget;
}

function setSuggestionSeparator(data) {
    const widget = widgets[data.widget];
    if (!widget?._setSuggestionSeparator || typeof data["suggestion separator"] !== "string")
        return false;
    widget._setSuggestionSeparator(data["suggestion separator"]);
    return true;
}

function setSuggestions(data) {
    const widget = widgets[data.widget];
    if (
        widget?._setSuggestions &&
        Array.isArray(data.suggestions) &&
        data.suggestions.every(value => typeof value === "string")
    ) {
        widget._setSuggestions(data.suggestions);
        return true;
    }
    return false;
}

// Protects local editing from delayed server echoes and applies the latest value when safe.
function initEditableText(widget) {
    widget._deferredText = null;
    widget._textInputPending = false;
    widget.setText = function (text) {
        if (document.activeElement === widget || widget._textInputPending) {
            widget._deferredText = text;
            return false;
        }
        widget._deferredText = null;
        if (widget.value != text) {
            widget.value = text;
            return true;
        }
        return false;
    };
    widget._applyDeferredText = function () {
        if (
            document.activeElement === widget ||
            widget._textInputPending ||
            widget._deferredText === null
        ) {
            return false;
        }
        const text = widget._deferredText;
        widget._deferredText = null;
        if (widget.value != text) {
            widget.value = text;
            return true;
        }
        return false;
    };
    addEvent(widget, "input", function () {
        widget._textInputPending = true;
        sendEventToServer(widget, "text input", { text: widget.value });
    });
    addEvent(widget, "blur", function () {
        widget._applyDeferredText();
    });
}

// Adds CSS text alignment support to text input controls.
function initTextAlignment(widget) {
    widget._setHorzAlignment = function (value) {
        switch (value) {
            case "center":
            case "right":
            case "justify":
                widget.style.textAlign = value;
                break;
            default:
                widget.style.textAlign = "left";
                break;
        }
    };
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
        if (
            widget.tagName === "A" &&
            (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)
        ) {
            return;
        }
        if (widget.tagName === "A" && widget._clientActions.click?.action === "go to page") {
            event.preventDefault();
        }
        if (widget._suppressClick) {
            widget._suppressClick = false;
            return;
        }
        sendEventToServer(widget, "click", processPointerEvent(widget, event));
        if (widget._onClick) {
            widget._onClick();
        }
    });
    addEvent(widget, "pointerenter", function (event) {
        widget._states.hovered = true;
        if (widget._events.click || widget._clientActions.click) {
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
        if (widget._events.click || widget._clientActions.click) {
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

function initCarouselGestures(widget) {
    let pointerId = null;
    let startX = 0;
    let startY = 0;
    let lastX = 0;
    let dragging = false;
    let direction = null;

    function width() {
        return Math.max(1, widget.getBoundingClientRect().width || widget.clientWidth || 1);
    }

    function translatedDistance(distance) {
        const atLeftEdge = widget._selectedIndex == 0 && distance > 0;
        const atRightEdge = widget._selectedIndex == widget._sources.length - 1 && distance < 0;
        if (atLeftEdge || atRightEdge) {
            const limit = width() / 3;
            return Math.max(-limit, Math.min(limit, distance / 3));
        }
        return Math.max(-width(), Math.min(width(), distance));
    }

    function moveImage(distance) {
        const offset = widget._selectedIndex * 100;
        widget._track.style.transform = "translateX(calc(-" + offset + "% + " + distance + "px))";
    }

    function settleImage() {
        widget._track.style.transition = "transform 180ms ease-out";
        widget._showSelectedImage();
    }

    addEvent(widget, "pointerdown", function (event) {
        if (pointerId != null || event.isPrimary === false || (event.button ?? 0) != 0) {
            return;
        }
        pointerId = event.pointerId ?? 1;
        startX = event.clientX;
        startY = event.clientY;
        lastX = startX;
        dragging = false;
        direction = null;
        widget._track.style.transition = "none";
        if (widget.setPointerCapture) {
            widget.setPointerCapture(pointerId);
        }
    });

    addEvent(widget, "pointermove", function (event) {
        if (pointerId == null || (event.pointerId ?? 1) != pointerId) {
            return;
        }
        lastX = event.clientX;
        const distance = lastX - startX;
        const verticalDistance = Math.abs(event.clientY - startY);
        const horizontalDistance = Math.abs(distance);
        if (direction == null && Math.hypot(horizontalDistance, verticalDistance) >= 6) {
            direction = horizontalDistance * 2 >= verticalDistance ? "horizontal" : "vertical";
        }
        if (direction != "horizontal") {
            return;
        }
        if (horizontalDistance > 5) {
            dragging = true;
        }
        if (dragging) {
            event.preventDefault();
            moveImage(translatedDistance(distance));
        }
    });

    function finish(event, cancelled) {
        if (pointerId == null || (event.pointerId ?? 1) != pointerId) {
            return;
        }
        const distance = lastX - startX;
        const threshold = Math.min(80, Math.max(24, width() / 5));
        let newIndex = widget._selectedIndex;
        if (!cancelled && dragging && Math.abs(distance) >= threshold) {
            if (distance < 0 && newIndex < widget._sources.length - 1) {
                newIndex++;
            } else if (distance > 0 && newIndex > 0) {
                newIndex--;
            }
        }
        if (dragging) {
            widget._suppressClick = true;
        }
        if (newIndex != widget._selectedIndex) {
            widget._selectedIndex = newIndex;
            widget._showSelectedImage();
            sendEventToServer(widget, "select", { index: newIndex });
        }
        settleImage();
        if (widget.releasePointerCapture && widget.hasPointerCapture?.(pointerId)) {
            widget.releasePointerCapture(pointerId);
        }
        pointerId = null;
        dragging = false;
        direction = null;
    }

    addEvent(widget, "pointerup", event => finish(event, false));
    addEvent(widget, "pointercancel", event => finish(event, true));
}

function initFocusEvents(widget) {
    addEvent(widget, "focus", function () {
        widget._states.focused = true;
        refreshWidget(widget);
        sendEventToServer(widget, "focus", {});
    });
    addEvent(widget, "blur", function () {
        widget._states.focused = false;
        refreshWidget(widget);
        sendEventToServer(widget, "blur", {});
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
    if (typeof clientDisposed !== "undefined" && clientDisposed) {
        return;
    }
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
    if (
        (typeof clientDisposed !== "undefined" && clientDisposed) ||
        (typeof clientFailed !== "undefined" && clientFailed) ||
        uploadRequestInFlight
    ) {
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
    sendRequest(
        {
            action: "upload chunk",
            client: clientId,
            widget: file.widget._id,
            fileId: file.id,
            chunkIndex: file.nextChunk,
            lastUpdate: "#" + lastProcessedUpdateId
        },
        function (data) {
            if (typeof clientDisposed !== "undefined" && clientDisposed) {
                return;
            }
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
            if (typeof responseHasClientError === "function" && responseHasClientError(receipt)) {
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
        "post",
        [
            {
                field: "chunk",
                name: "chunk.bin",
                data: chunk
            }
        ]
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
            /fill\s*=\s*(['"])(?:black|#000|#000000|rgb\s*\(\s*0\s*,\s*0\s*,\s*0\s*\))\1/gi,
            'fill="' + color + '"'
        )
        .replace(
            /fill\s*=\s*(['"])(?:white|#fff|#ffffff|rgb\s*\(\s*255\s*,\s*255\s*,\s*255\s*\))\1/gi,
            'fill="' + bgColor + '"'
        );
    return prefix + encodeURIComponent(decoded);
}

// Suppress only clicks produced by a gesture, including clicks on nested controls.
function initGestureClicks(widget) {
    let suppressed = false;
    widget._suppressGestureClick = () => {
        suppressed = true;
    };
    widget.addEventListener(
        "pointerdown",
        () => {
            suppressed = false;
        },
        true
    );
    widget.addEventListener(
        "click",
        event => {
            if (suppressed) {
                suppressed = false;
                event.preventDefault();
                event.stopImmediatePropagation();
            }
        },
        true
    );
    widget.addEventListener("dragstart", event => event.preventDefault());
}

function releaseGesturePointer(widget, id) {
    if (widget.hasPointerCapture?.(id)) widget.releasePointerCapture(id);
}

function createSortableSection() {
    const widget = widgetsLibrary.section();
    widget._revision = 0;
    widget._pendingReorder = false;
    widget._animationDuration = 0;
    widget.dataset.sortable = "true";
    let gesture = null;
    const animations = new Map();
    initGestureClicks(widget);
    widget._layoutChild = child => {
        child.style.touchAction = "none";
        child.style.userSelect = "none";
        if (!child.hasAttribute("tabindex")) child.tabIndex = 0;
        child.setAttribute("aria-keyshortcuts", "Alt+ArrowLeft Alt+ArrowRight");
    };
    const interactive = target =>
        target.closest(
            "button,a,input,select,textarea,[contenteditable]:not([contenteditable=false]),[data-sortable],[data-zoom]"
        );
    function directChild(target) {
        while (target && target.parentElement !== widget) target = target.parentElement;
        return target;
    }
    function positions() {
        return new Map(
            Array.from(widget.children, child => [child, child.getBoundingClientRect()])
        );
    }
    function stopAnimations() {
        for (const entry of animations.values()) {
            entry.animation.cancel();
            entry.restore();
        }
        animations.clear();
    }
    widget._stopSortAnimations = stopAnimations;
    // FLIP: sample visual positions, change layout once, then animate back from the old positions.
    function reflow(change, raisedChild = null) {
        const before = positions();
        stopAnimations();
        change();
        const duration = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches
            ? 0
            : widget._animationDuration;
        if (!duration) return;
        for (const child of widget.children) {
            const old = before.get(child);
            if (!old || !child.animate) continue;
            const rect = child.getBoundingClientRect();
            const dx = old.left - rect.left;
            const dy = old.top - rect.top;
            if (Math.abs(dx) < 0.1 && Math.abs(dy) < 0.1) continue;
            const transform = getComputedStyle(child).transform;
            const saved = { position: child.style.position, zIndex: child.style.zIndex };
            if (child === raisedChild) {
                child.style.position = "relative";
                child.style.zIndex = "1000";
            }
            const restore = () => {
                if (child === raisedChild) Object.assign(child.style, saved);
            };
            const animation = child.animate(
                [
                    {
                        transform: `translate(${dx}px, ${dy}px) ${transform === "none" ? "" : transform}`
                    },
                    { transform }
                ],
                { duration, easing: "cubic-bezier(0.2, 0, 0, 1)" }
            );
            animations.set(child, { animation, restore });
            animation.onfinish = () => {
                if (animations.get(child)?.animation === animation) {
                    animations.delete(child);
                    restore();
                }
            };
        }
    }
    function clearGesture() {
        if (!gesture) return null;
        const current = gesture;
        gesture = null;
        Object.assign(current.child.style, current.style, { transition: "none" });
        current.child.getBoundingClientRect();
        current.child.style.transition = current.style.transition;
        if (current.moved) widget._suppressGestureClick();
        releaseGesturePointer(widget, current.id);
        return current;
    }
    function reportMove(child) {
        widget._pendingReorder = true;
        sendEventToServer(widget, "reorder", {
            revision: widget._revision++,
            child: child._id,
            before: child.nextElementSibling?._id || ""
        });
    }
    function finish(cancelled) {
        if (!gesture) return;
        const current = gesture;
        reflow(() => {
            clearGesture();
            if (!cancelled && current.moved && current.before !== current.child) {
                widget.insertBefore(current.child, current.before);
            }
        }, current.child);
        if (
            !cancelled &&
            current.moved &&
            Array.from(widget.children).some((child, index) => child !== current.order[index])
        ) {
            reportMove(current.child);
        }
    }
    widget._cancelGesture = () => finish(true);
    widget._applySortOrder = children => {
        // An acknowledgement must not interrupt the drop animation already running locally.
        if (!gesture && children.every((child, index) => child === widget.children[index])) return;
        const focused = document.activeElement;
        reflow(() => {
            clearGesture();
            for (const child of children) widget.appendChild(child);
            if (focused && widget.contains(focused)) focused.focus({ preventScroll: true });
        });
    };
    widget._onDetached = () => {
        clearGesture();
        stopAnimations();
    };
    widget.addEventListener("pointerdown", event => {
        if (gesture) {
            finish(true);
            return;
        }
        if (widget._pendingReorder || event.button !== 0 || event.isPrimary === false) return;
        const control = interactive(event.target);
        if (control && control !== widget) return;
        const child = directChild(event.target);
        if (!child) return;
        stopAnimations();
        const rect = child.getBoundingClientRect();
        gesture = {
            id: event.pointerId,
            child,
            x: event.clientX,
            y: event.clientY,
            offsetX: event.clientX - rect.left,
            offsetY: event.clientY - rect.top,
            dx: 0,
            dy: 0,
            moved: false,
            before: child,
            order: Array.from(widget.children),
            transform: getComputedStyle(child).transform,
            style: {
                transform: child.style.transform,
                position: child.style.position,
                zIndex: child.style.zIndex,
                transition: child.style.transition
            }
        };
        widget.setPointerCapture?.(event.pointerId);
    });
    function move(event) {
        if (!gesture || event.pointerId !== gesture.id) return;
        if (!gesture.moved && Math.hypot(event.clientX - gesture.x, event.clientY - gesture.y) < 6)
            return;
        const current = gesture;
        const child = current.child;
        const rect = child.getBoundingClientRect();
        const base = {
            left: rect.left - current.dx,
            top: rect.top - current.dy,
            right: rect.right - current.dx,
            bottom: rect.bottom - current.dy,
            width: rect.width
        };
        current.moved = true;
        current.dx = event.clientX - current.offsetX - base.left;
        current.dy = event.clientY - current.offsetY - base.top;
        Object.assign(child.style, {
            position: "relative",
            zIndex: "1000",
            transition: "none",
            transform: `translate(${current.dx}px, ${current.dy}px) ${current.transform === "none" ? "" : current.transform}`
        });
        event.preventDefault();
        // Keep every layout slot intact until release, including the dragged card's own slot.
        let nearest = child;
        let targetRect = base;
        let distance = Infinity;
        for (const candidate of widget.children) {
            const box = candidate === child ? base : candidate.getBoundingClientRect();
            const dx = Math.max(box.left - event.clientX, 0, event.clientX - box.right);
            const dy = Math.max(box.top - event.clientY, 0, event.clientY - box.bottom);
            const score = dx * dx + dy * dy;
            if (score < distance) {
                distance = score;
                nearest = candidate;
                targetRect = box;
            }
        }
        if (nearest === child) {
            current.before = child;
            return;
        }
        const after =
            event.clientY > targetRect.bottom ||
            (event.clientY >= targetRect.top &&
                event.clientX > targetRect.left + targetRect.width / 2);
        current.before = after ? nearest.nextElementSibling : nearest;
    }
    widget.addEventListener("pointermove", move);
    widget.addEventListener("pointerup", event => {
        if (event.pointerId === gesture?.id) {
            move(event);
            finish(false);
        }
    });
    for (const type of ["pointercancel", "lostpointercapture"]) {
        widget.addEventListener(type, event => {
            if (event.pointerId === gesture?.id) finish(true);
        });
    }
    widget.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            finish(true);
            return;
        }
        if (!event.altKey || !["ArrowLeft", "ArrowRight"].includes(event.key)) return;
        const child = directChild(event.target);
        if (!child || event.target !== child || gesture || widget._pendingReorder) return;
        const next =
            event.key === "ArrowLeft" ? child.previousElementSibling : child.nextElementSibling;
        if (!next) return;
        event.preventDefault();
        reflow(() => {
            widget.insertBefore(child, event.key === "ArrowLeft" ? next : next.nextElementSibling);
            child.focus({ preventScroll: true });
        });
        reportMove(child);
    });
    return widget;
}

function setAnimationDuration(data) {
    const widget = widgets[data.widget];
    if (
        !widget?._applySortOrder ||
        !Number.isInteger(data["animation duration"]) ||
        data["animation duration"] < 0
    )
        return false;
    widget._animationDuration = data["animation duration"];
    if (!data["animation duration"]) widget._stopSortAnimations();
    return true;
}

function setChildOrder(data) {
    const widget = widgets[data.widget];
    if (
        !widget?._cancelGesture ||
        !Array.isArray(data.children) ||
        !Number.isInteger(data.revision) ||
        data.revision < 0
    )
        return false;
    const children = data.children.map(id => widgets[id]);
    if (
        children.length !== widget.children.length ||
        new Set(children).size !== children.length ||
        children.some(child => !child || child.parentElement !== widget)
    )
        return false;
    widget._applySortOrder(children);
    widget._revision = data.revision;
    widget._pendingReorder = false;
    return true;
}

function createZoomDecorator() {
    const widget = document.createElement("span");
    const stage = document.createElement("span");
    widget.dataset.zoom = "true";
    Object.assign(widget.style, {
        display: "inline-block",
        overflow: "hidden",
        touchAction: "none",
        userSelect: "none",
        verticalAlign: "middle"
    });
    Object.assign(stage.style, {
        display: "inline-block",
        transformOrigin: "0 0",
        verticalAlign: "top"
    });
    widget.appendChild(stage);
    widget._stage = stage;
    widget._childHost = stage;
    widget._maxScale = 1;
    widget._fitContent = false;
    let scale = 1;
    let x = 0;
    let y = 0;
    const pointers = new Map();
    const captures = new Map();
    let moved = false;
    initGestureClicks(widget);
    function render() {
        const width = stage.offsetWidth;
        const height = stage.offsetHeight;
        const fit =
            widget._fitContent &&
            width > 0 &&
            height > 0 &&
            widget.clientWidth > 0 &&
            widget.clientHeight > 0
                ? Math.min(widget.clientWidth / width, widget.clientHeight / height)
                : 1;
        const actualScale = fit * scale;
        function clamp(offset, viewport, content) {
            if (widget._fitContent && content <= viewport) return (viewport - content) / 2;
            return Math.min(0, Math.max(Math.min(0, viewport - content), offset));
        }
        x = clamp(x, widget.clientWidth, width * actualScale);
        y = clamp(y, widget.clientHeight, height * actualScale);
        if (scale === 1 && !widget._fitContent) {
            x = 0;
            y = 0;
        }
        stage.style.transform = `translate(${x}px, ${y}px) scale(${actualScale})`;
    }
    function clearPointers() {
        const ids = Array.from(pointers.keys());
        pointers.clear();
        for (const id of ids) {
            releaseGesturePointer(captures.get(id) || widget, id);
        }
        captures.clear();
    }
    widget._resetZoom = () => {
        clearPointers();
        scale = 1;
        x = 0;
        y = 0;
        moved = false;
        render();
    };
    widget._setChild = child => {
        widget._resetZoom();
        stage.replaceChildren(child);
    };
    function localPoint(event) {
        const rect = widget.getBoundingClientRect();
        return {
            x: event.clientX - rect.left - widget.clientLeft,
            y: event.clientY - rect.top - widget.clientTop
        };
    }
    function zoomAt(next, oldCenter, newCenter = oldCenter) {
        next = Math.max(1, Math.min(widget._maxScale, next));
        const ratio = next / scale;
        x = newCenter.x - (oldCenter.x - x) * ratio;
        y = newCenter.y - (oldCenter.y - y) * ratio;
        scale = next;
        render();
    }
    widget.addEventListener(
        "wheel",
        event => {
            event.preventDefault();
            const unit =
                event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? widget.clientHeight : 1;
            zoomAt(
                scale * Math.exp(Math.max(-2, Math.min(2, -event.deltaY * unit * 0.002))),
                localPoint(event)
            );
        },
        { passive: false }
    );
    widget.addEventListener("pointerdown", event => {
        if (event.button !== 0 || pointers.size >= 2) return;
        if (!pointers.size) moved = false;
        pointers.set(event.pointerId, localPoint(event));
        const target = event.target;
        captures.set(event.pointerId, target);
        target.setPointerCapture?.(event.pointerId);
    });
    widget.addEventListener("pointermove", event => {
        const previous = pointers.get(event.pointerId);
        if (!previous) return;
        const point = localPoint(event);
        if (pointers.size === 2) {
            const other = Array.from(pointers.entries()).find(([id]) => id !== event.pointerId)[1];
            const oldDistance = Math.hypot(previous.x - other.x, previous.y - other.y);
            const newDistance = Math.hypot(point.x - other.x, point.y - other.y);
            if (oldDistance > 0 && newDistance > 0) {
                zoomAt(
                    (scale * newDistance) / oldDistance,
                    { x: (previous.x + other.x) / 2, y: (previous.y + other.y) / 2 },
                    { x: (point.x + other.x) / 2, y: (point.y + other.y) / 2 }
                );
                moved = true;
            }
        } else if (scale > 1) {
            x += point.x - previous.x;
            y += point.y - previous.y;
            if (Math.hypot(point.x - previous.x, point.y - previous.y) > 0) moved = true;
            render();
        }
        pointers.set(event.pointerId, point);
        if (moved) event.preventDefault();
    });
    function finish(event) {
        if (!pointers.has(event.pointerId)) return;
        pointers.delete(event.pointerId);
        if (moved) widget._suppressGestureClick();
        const target = captures.get(event.pointerId) || widget;
        captures.delete(event.pointerId);
        releaseGesturePointer(target, event.pointerId);
    }
    for (const type of ["pointerup", "pointercancel", "lostpointercapture"]) {
        widget.addEventListener(type, finish);
    }
    widget.addEventListener("load", render, true);
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(render);
    widget._onAttached = () => {
        observer?.observe(widget);
        observer?.observe(stage);
        render();
    };
    widget._onDetached = () => {
        clearPointers();
        observer?.disconnect();
    };
    widget._resetZoom();
    return widget;
}

function setMaxScale(data) {
    const widget = widgets[data.widget];
    if (!widget?._resetZoom || !Number.isFinite(data["max scale"]) || data["max scale"] < 1)
        return false;
    widget._maxScale = data["max scale"];
    widget._resetZoom();
    return true;
}

function resetZoom(data) {
    const widget = widgets[data.widget];
    if (!widget?._resetZoom) return false;
    widget._resetZoom();
    return true;
}

function setCloseOnOutsideClick(data) {
    const widget = widgets[data.widget];
    const enabled = data["close on outside click"];
    if (!widget?._backdrop || typeof enabled !== "boolean") return false;
    widget._closeOnOutsideClick = enabled;
    return true;
}

function setFitContent(data) {
    const widget = widgets[data.widget],
        enabled = data["fit content"];
    if (!widget?._resetZoom || typeof enabled !== "boolean") return false;
    widget._fitContent = enabled;
    widget._stage.style.display = enabled ? "inline-flex" : "inline-block";
    widget._stage.style.width = enabled ? "max-content" : "";
    widget._resetZoom();
    return true;
}
