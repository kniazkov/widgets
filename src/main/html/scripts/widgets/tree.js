/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// This per-runtime cache is separate from whole pages retained for Back/Forward.
const detachedWidgets = new Map();
const detachedWidgetLimit = 128;
let detachedWidgetsEvicted = false;

function registeredSubtree(widget) {
    const nodes = [widget];
    for (const child of widget.querySelectorAll("*")) {
        if (widgets[child._id] === child) nodes.push(child);
    }
    return nodes;
}

function pauseWidgetSubtree(widget) {
    for (const node of registeredSubtree(widget).reverse()) {
        if (!node._treePaused) {
            node._treePaused = true;
            node._onDetached?.();
        }
    }
}

function resumeWidgetSubtree(widget) {
    for (const node of registeredSubtree(widget)) {
        node._treePaused = false;
        node._onAttached?.();
    }
}

function retainDetachedWidget(widget) {
    pauseWidgetSubtree(widget);
    widget.remove();
    refreshAccessibleRelations();
    detachedWidgets.delete(widget);
    detachedWidgets.set(widget, true);
}

function disposeWidgetSubtree(widget, preserveElement = false) {
    const nodes = registeredSubtree(widget).reverse();
    for (const node of nodes) {
        if (!node._treePaused) node._onDetached?.();
        node._onDisposed?.();
        node._listenerScope?.abort();
        node._events = {};
        node._clientActions = {};
        discardWidgetUploads(node);
        detachedWidgets.delete(node);
        forgetAccessibleWidget(node);
        if (widgets[node._id] === node) delete widgets[node._id];
    }
    if (preserveElement) widget.replaceChildren();
    else widget.remove();
    refreshAccessibleRelations();
}

function disposeWidget(data) {
    const widget = widgets[data.widget];
    if (!widget) return true; // Duplicate disposal and already evicted detachments are harmless.
    if (
        widget === document.body ||
        (typeof pageContext !== "undefined" && widget === pageContext?.root)
    )
        return false;
    disposeWidgetSubtree(widget);
    refreshCountdowns();
    return true;
}

function disposeAllWidgets() {
    const root = pageContext?.root || document.body;
    if (widgets[root._id] === root) disposeWidgetSubtree(root, true);
    for (const widget of Object.values(widgets)) {
        if (widgets[widget._id] === widget) disposeWidgetSubtree(widget);
    }
    detachedWidgets.clear();
    detachedWidgetsEvicted = false;
    refreshCountdowns();
}

function trimDetachedWidgets() {
    let count = 0;
    const roots = new Map();
    for (const widget of detachedWidgets.keys()) {
        const size = registeredSubtree(widget).length;
        count += size;
        roots.set(widget, size);
    }
    for (const [widget, size] of roots) {
        if (count <= detachedWidgetLimit) break;
        disposeWidgetSubtree(widget);
        count -= size;
        detachedWidgetsEvicted = true;
    }
    if (roots.size > 0) refreshCountdowns();
}

function finishWidgetAttachment(widget) {
    detachedWidgets.delete(widget);
    resumeWidgetSubtree(widget);
    refreshAccessibleRelations();
    refreshCountdowns();
}

function canAttachWidgets(widget, container) {
    if (widget && container) return true;
    // No unbounded tombstone list is kept. If the cache has evicted DOM, a
    // missing attachment target needs a fresh runtime, not a partial tree.
    if (detachedWidgetsEvicted) reloadCurrentPage();
    return false;
}

// State-preserving native reparenting when the runtime supports it.
function placeWidget(widget, host, before = null) {
    if (widget.parentNode && widget.isConnected && host.isConnected && host.moveBefore) {
        host.moveBefore(widget, before);
    } else {
        host.insertBefore(widget, before);
    }
}

function setChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (!canAttachWidgets(widget, container)) return false;
    const host = container._childHost || container;
    // Move out first in case the replacement is nested in the old child.
    // Otherwise retaining the old subtree could suspend or evict the replacement too.
    if (widget.parentNode !== host) placeWidget(widget, host);
    for (const old of Array.from(host.children)) {
        if (old !== widget && widgets[old._id] === old) retainDetachedWidget(old);
    }
    if (container._setChild) container._setChild(widget);
    else {
        for (const old of Array.from(host.childNodes)) {
            if (old !== widget) old.remove();
        }
    }
    finishWidgetAttachment(widget);
    log("Widget " + data.widget + " is set as a child of widget " + data.container + ".");
    return true;
}

function appendChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (!canAttachWidgets(widget, container)) return false;
    placeWidget(widget, container._childHost || container);
    container._layoutChild?.(widget);
    finishWidgetAttachment(widget);
    log("Widget " + data.widget + " is added as a child of widget " + data.container + ".");
    return true;
}

function insertChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    const index = data.index;
    if (!canAttachWidgets(widget, container)) return false;
    const host = container._childHost || container;
    const children = Array.from(host.children).filter(
        child => !child._isWidgetBackdrop && !child._isModalDescription
    );
    if (!Number.isInteger(index) || index < 0 || index > children.length) return false;
    const next = children[index];
    placeWidget(widget, host, next?._backdrop?.parentNode === host ? next._backdrop : next || null);
    container._layoutChild?.(widget);
    finishWidgetAttachment(widget);
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

function removeChildWidget(data) {
    const widget = widgets[data.widget];
    const container = widgets[data.container];
    if (!widget || !container) return true;
    const host = container._childHost || container;
    if (widget.parentNode !== host) return true;
    retainDetachedWidget(widget);
    log("Widget " + data.widget + " is removed from parent widget " + data.container + ".");
    refreshCountdowns();
    return true;
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
