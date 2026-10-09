/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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

// CSS keeps overlays attached to the content during intrinsic and responsive size changes.
function setFitToFirstChild(data) {
    const widget = widgets[data.widget];
    const value = data["fit to first child"];
    if (!widget?.classList.contains("widgets-overlay-stack") || typeof value !== "boolean") {
        return false;
    }
    widget.classList.toggle("widgets-overlay-fit-first", value);
    return true;
}
