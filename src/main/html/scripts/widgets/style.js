/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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

// Rendering helpers.
function composeColor(rgb) {
    if (typeof rgb.a == "number") {
        return "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + rgb.a + ")";
    } else {
        return "rgb(" + rgb.r + "," + rgb.g + "," + rgb.b + ")";
    }
}
