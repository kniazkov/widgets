/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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
