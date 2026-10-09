/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

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
