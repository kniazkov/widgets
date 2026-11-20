/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function setWidth(data) {
    const widget = widgets[data.widget];
    const value = data.width;
    if (widget && typeof value == "string") {
        widget.style.width = value || widget._defaultWidth || "";
        if (widget._backdrop) widget.toggleAttribute("data-intrinsic-width", !value);
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
        widget.style.height = value || widget._defaultHeight || "";
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

function createAdaptiveLayout(kind) {
    const widget = document.createElement("div");
    widget.className = "widgets-adaptive-layout widgets-" + kind;
    initPointerEvents(widget, true);
    if (kind !== "form-layout") return widget;
    let width = 0;
    let frame = null;
    widget._formLayoutBreakpoint = 520;
    function render() {
        widget.toggleAttribute(
            "data-label-columns",
            widget.isConnected && !widget._treePaused && width >= widget._formLayoutBreakpoint
        );
    }
    const observer =
        typeof ResizeObserver === "undefined"
            ? null
            : new ResizeObserver(entries => {
                  if (!widget.isConnected || widget._treePaused) return;
                  const next = entries[0].contentRect.width;
                  if (next === width) return;
                  width = next;
                  // Reflow changes height; do not mutate geometry inside ResizeObserver delivery.
                  if (frame === null) {
                      frame = requestAnimationFrame(() => {
                          frame = null;
                          render();
                      });
                  }
              });
    widget._renderFormLayout = render;
    widget._onAttached = () => observer?.observe(widget);
    widget._onDetached = () => {
        observer?.disconnect();
        if (frame !== null) cancelAnimationFrame(frame);
        frame = null;
        width = 0;
        render();
    };
    return widget;
}

function setAdaptiveLayout(data, property) {
    const widget = widgets[data.widget];
    const value = data[property];
    const type = widget?._widgetType;
    const within = (v, low, high) => Number.isInteger(v) && v >= low && v <= high;
    if (property === "layout gap" && ["stack", "responsive grid", "form layout"].includes(type)) {
        if (!within(value, 0, 128)) return false;
        widget.style.setProperty("--widgets-layout-gap", value + "px");
    } else if (property === "stack direction" && type === "stack") {
        if (!["vertical", "horizontal"].includes(value)) return false;
        widget.toggleAttribute("data-inline-axis", value === "horizontal");
    } else if (property === "stack alignment" && type === "stack") {
        if (!["start", "center", "end", "stretch"].includes(value)) return false;
        widget.style.alignItems = value;
    } else if (property === "stack wrap" && type === "stack") {
        if (typeof value !== "boolean") return false;
        widget.toggleAttribute("data-no-wrap", !value);
    } else if (property === "grid min width" && type === "responsive grid") {
        if (!within(value, 40, 2048)) return false;
        widget.style.setProperty("--widgets-grid-min", value + "px");
    } else if (property === "grid max width" && type === "responsive grid") {
        if (value !== 0 && !within(value, 40, 2048)) return false;
        const max = value === 0 ? "1fr" : "max(var(--widgets-grid-min, 160px), " + value + "px)";
        widget.style.setProperty("--widgets-grid-max", max);
    } else if (property === "form breakpoint" && type === "form layout") {
        if (!within(value, 240, 2048)) return false;
        widget._formLayoutBreakpoint = value;
        widget._renderFormLayout();
    } else {
        return false;
    }
    return true;
}
