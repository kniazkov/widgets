/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createBreadcrumbs() {
    const widget = document.createElement("nav");
    widget.className = "widgets-breadcrumbs";
    const list = document.createElement("div");
    list.setAttribute("role", "list");
    widget.append(list);
    widget._childHost = list;
    widget._layoutChild = () => {
        for (const [index, child] of Array.from(list.children).entries()) {
            child.setAttribute("role", "listitem");
            if (index === list.children.length - 1) child.setAttribute("aria-current", "page");
            else child.removeAttribute("aria-current");
        }
    };
    return widget;
}

function navigationId(node) {
    node.id ||= "widgets-semantic-" + node._id;
    return node.id;
}

function navigationKeydown(widget, event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey)
        return;
    const state = widget._navigation;
    if (!state || widget._treePaused) return;
    const tabs = state.kind === "tabs";
    const available = state.items.filter(item => item.visible && item.enabled);
    const index = available.findIndex(item => widgets[item.button] === event.target);
    if (index < 0 || !available.length) return;
    let next;
    if (event.key === "Home") next = 0;
    else if (event.key === "End") next = available.length - 1;
    else if (event.key === (tabs ? "ArrowRight" : "ArrowDown"))
        next = (index + 1) % available.length;
    else if (event.key === (tabs ? "ArrowLeft" : "ArrowUp"))
        next = (index + available.length - 1) % available.length;
    else return;
    event.preventDefault();
    if (tabs) {
        for (const item of available) widgets[item.button].tabIndex = -1;
        widgets[available[next].button].tabIndex = 0;
    }
    widgets[available[next].button].focus();
}

// Validate the complete snapshot before changing DOM, including ownership and unique references.
function setNavigation(data) {
    const widget = widgets[data.widget];
    const state = data.snapshot;
    if (
        !widget ||
        !state ||
        !["tabs", "accordion"].includes(state.kind) ||
        !Array.isArray(state.items)
    )
        return false;
    const tabs = state.kind === "tabs";
    const headers = widgets[state.headers];
    if (tabs && (!headers || headers.parentElement !== widget)) return false;
    const keys = new Set();
    const refs = new Set();
    for (const item of state.items) {
        const button = widgets[item.button];
        const panel = widgets[item.panel];
        const row = widgets[item.headers];
        if (
            typeof item.key !== "string" ||
            !item.key.trim() ||
            keys.has(item.key) ||
            ![item.selected, item.visible, item.enabled].every(
                value => typeof value === "boolean"
            ) ||
            (item.selected && (!item.visible || !item.enabled)) ||
            !button ||
            button.tagName !== "BUTTON" ||
            !panel ||
            !row ||
            button.parentElement !== row ||
            row.parentElement !== widget ||
            panel.parentElement !== widget ||
            refs.has(button) ||
            refs.has(panel) ||
            (tabs && row !== headers)
        )
            return false;
        keys.add(item.key);
        refs.add(button);
        refs.add(panel);
    }
    const replacing = state.replacing ?? "";
    if (typeof replacing !== "string" || (replacing && !keys.has(replacing))) return false;
    const available = state.items.filter(item => item.visible && item.enabled);
    if (tabs && state.items.filter(item => item.selected).length !== (available.length ? 1 : 0))
        return false;
    const active = document.activeElement;
    const old = widget._navigation;
    const affected = old?.items.find(item => {
        const button = widgets[item.button];
        const panel = widgets[item.panel];
        return button === active || panel?.contains(active);
    });
    const current = affected && state.items.find(item => item.key === affected.key);
    const lostFocus =
        affected &&
        (!current ||
            !current.visible ||
            !current.enabled ||
            (widgets[affected.panel]?.contains(active) &&
                (!current.selected || replacing === affected.key)));
    widget.setAttribute("role", "group");
    widget.classList.add(tabs ? "widgets-tabs" : "widgets-accordion");
    if (tabs) {
        headers.setAttribute("role", "tablist");
        headers.setAttribute("aria-labelledby", navigationId(widget));
        // The outer accessible name labels the list without copying it on each update.
    }
    const focusedHeader = available.find(item => widgets[item.button] === active);
    for (const item of state.items) {
        const button = widgets[item.button];
        const panel = widgets[item.panel];
        const row = widgets[item.headers];
        button.disabled = !item.enabled;
        button.style.display = item.visible ? button._display || "" : "none";
        panel.style.display = item.selected ? panel._display || "" : "none";
        if (!tabs) row.style.display = item.visible ? row._display || "" : "none";
        const buttonId = navigationId(button);
        const panelId = navigationId(panel);
        button.setAttribute("aria-controls", panelId);
        panel.setAttribute("aria-labelledby", buttonId);
        panel.setAttribute("role", tabs ? "tabpanel" : "region");
        if (tabs) {
            button.setAttribute("role", "tab");
            button.setAttribute("aria-selected", String(item.selected));
            button.tabIndex = (focusedHeader ? focusedHeader === item : item.selected) ? 0 : -1;
            placeWidget(button, headers);
        } else {
            button.setAttribute("aria-expanded", String(item.selected));
            placeWidget(row, widget);
        }
        placeWidget(panel, widget);
    }
    widget._navigation = state;
    if (!widget._navigationListening) {
        widget._navigationListening = true;
        listenWidget(widget, "keydown", event => navigationKeydown(widget, event));
        widget._onDisposed = () => {
            widget._navigation = null;
        };
    }
    if (lostFocus) {
        const target = state.items.find(item => item.selected) || available[0];
        if (target) widgets[target.button].focus();
        else {
            widget.tabIndex = -1;
            widget.focus();
        }
    } else if (active?.isConnected && document.activeElement !== active) {
        active.focus({ preventScroll: true });
    }
    return true;
}
