/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// The registry connects server-side widget IDs to their browser DOM nodes.
const widgets = {};

// Protocol widget type names map to factories that initialize the matching DOM element.
const widgetsLibrary = {
    root: function () {
        return typeof page === "undefined" ? document.body : page.root;
    },
    "labeled choice row": function () {
        const widget = widgetsLibrary.section();
        widget.className = "widgets-labeled-choice-row";
        widget.style.flexWrap = "nowrap";
        return widget;
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
    "horizontal line": function () {
        const widget = document.createElement("hr");
        // Use currentColor so the regular reactive color property paints the line.
        widget.style.border = "0";
        widget.style.padding = "0";
        widget.style.boxSizing = "border-box";
        widget.style.backgroundColor = "currentColor";
        widget.style.flexShrink = "0";
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
    markdown: createMarkdown,
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
    "no-wrap": function () {
        const widget = document.createElement("span");
        widget.classList.add("widgets-no-wrap");
        widget.style.display = "inline-block";
        widget.style.whiteSpace = "nowrap";
        widget.style.flexShrink = "0";
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
        widget.classList.add("widgets-overlay-stack");
        widget.style.position = "relative";
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
