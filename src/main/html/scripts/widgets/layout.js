/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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
