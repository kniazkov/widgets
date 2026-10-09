/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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

// A detached page keeps its own metadata; only its navigation owner can publish it.
function setDocumentMetadata(data, name) {
    const widget = widgets[data.widget];
    const value = data["document " + name];
    const root = typeof page === "undefined" ? document.body : page.root;
    if (!widget || widget !== root || typeof value !== "string") return false;
    widget._documentMetadata ||= {};
    widget._documentMetadata[name] = value;
    if (typeof page === "undefined") {
        applyDocumentMetadata({ ...readDocumentMetadata(), ...widget._documentMetadata });
    } else {
        page.metadataChanged?.();
    }
    return true;
}
