/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Each page runtime owns its overlays. Native manual popovers escape clipping and transforms
// without reparenting server widgets or breaking the existing modal ancestry/focus rules.
const anchoredOverlays = new Set();
const anchoredStack = [];
let anchoredScope;
let anchoredObserver;
let anchoredFrame = 0;

function overlayViewport() {
    const view = window.visualViewport;
    return {
        left: view?.offsetLeft || 0,
        top: view?.offsetTop || 0,
        width: view?.width || window.innerWidth,
        height: view?.height || window.innerHeight
    };
}

function overlayAnchorAvailable(widget) {
    const anchor = widget._anchor;
    const root = modalRoot();
    if (
        !anchor ||
        !widget.isConnected ||
        !root.contains(widget) ||
        !root.contains(anchor) ||
        widget._treePaused ||
        widget.style.display === "none" ||
        !visibleModalElement(widget.parentElement) ||
        anchor._treePaused ||
        anchor.matches(":disabled") ||
        anchor.closest("[inert]") ||
        widget.closest("[inert]") ||
        (activeModal && (!activeModal.contains(anchor) || !activeModal.contains(widget)))
    )
        return false;
    if (!anchor.getClientRects().length || getComputedStyle(anchor).visibility === "hidden")
        return false;
    const rect = anchor.getBoundingClientRect();
    const view = overlayViewport();
    let left = view.left,
        top = view.top,
        right = left + view.width,
        bottom = top + view.height;
    for (
        let parent = anchor.parentElement;
        parent && parent !== root;
        parent = parent.parentElement
    ) {
        const css = getComputedStyle(parent);
        if (/(auto|scroll|hidden|clip)/.test(css.overflow + css.overflowX + css.overflowY)) {
            const box = parent.getBoundingClientRect();
            left = Math.max(left, box.left);
            right = Math.min(right, box.right);
            top = Math.max(top, box.top);
            bottom = Math.min(bottom, box.bottom);
        }
    }
    return rect.right > left && rect.left < right && rect.bottom > top && rect.top < bottom;
}

// Pure collision calculation is shared by real layout and deterministic viewport tests.
function anchoredPosition(anchor, size, viewport, preferred, gap = 6, padding = 8) {
    const left = viewport.left + padding,
        top = viewport.top + padding;
    const right = viewport.left + viewport.width - padding;
    const bottom = viewport.top + viewport.height - padding;
    const space = {
        bottom: bottom - anchor.bottom - gap,
        top: anchor.top - top - gap,
        left: anchor.left - left - gap,
        right: right - anchor.right - gap
    };
    const opposite = { bottom: "top", top: "bottom", left: "right", right: "left" };
    const extent = preferred === "top" || preferred === "bottom" ? size.height : size.width;
    let side = preferred;
    if (space[side] < extent && space[opposite[side]] > space[side]) side = opposite[side];
    let x = anchor.left,
        y = anchor.bottom + gap;
    if (side === "top") y = anchor.top - size.height - gap;
    if (side === "left") {
        x = anchor.left - size.width - gap;
        y = anchor.top;
    }
    if (side === "right") {
        x = anchor.right + gap;
        y = anchor.top;
    }
    return {
        side,
        x: Math.max(left, Math.min(x, right - size.width)),
        y: Math.max(top, Math.min(y, bottom - size.height))
    };
}

function overlayCommand(widget, action, key = "") {
    const state = widget._overlay;
    if (!state || widget._overlayPending || widget._treePaused) return false;
    if (action === "open" && !overlayAnchorAvailable(widget)) return false;
    widget._overlayPending = true;
    widget._overlayPendingAction = action;
    sendEventToServer(widget, "overlay command", { action, key, revision: state.revision });
    return true;
}

function hideAnchoredOverlay(widget, restore = false) {
    const wasOpen = widget._overlayShown;
    widget._overlayShown = false;
    const index = anchoredStack.indexOf(widget);
    if (index >= 0) anchoredStack.splice(index, 1);
    if (widget.matches(":popover-open")) widget.hidePopover();
    widget._anchor?.setAttribute("aria-expanded", "false");
    if (widget._overlay?.kind === "tooltip") widget._anchor?.removeAttribute("aria-expanded");
    if (wasOpen && restore) {
        if (!focusModalElement(widget._anchor)) {
            const target = activeModal || modalRoot();
            if (!target.hasAttribute("tabindex")) target.tabIndex = -1;
            focusModalElement(target);
        }
    }
}

function closeAnchoredOverlay(widget, restore = false) {
    clearTimeout(widget._overlayTimer);
    widget._overlayIntent = false;
    hideAnchoredOverlay(widget, restore);
    if (widget._overlay?.open) overlayCommand(widget, "close");
}

function refreshAnchoredOverlay(widget) {
    const state = widget._overlay;
    if (!state) return;
    if (!widget._anchorScope && widget.isConnected && modalRoot().contains(widget._anchor))
        attachOverlayAnchor(widget);
    if (!overlayAnchorAvailable(widget)) {
        if (widget._anchor && !modalRoot().contains(widget._anchor)) detachOverlayAnchor(widget);
        if (widget._overlayPendingAction !== "close") widget._overlayPending = false;
        if (state.open) closeAnchoredOverlay(widget, widget.contains(document.activeElement));
        else {
            clearTimeout(widget._overlayTimer);
            widget._overlayIntent = undefined;
            hideAnchoredOverlay(widget);
        }
        return;
    }
    if (!widget._anchorScope) attachOverlayAnchor(widget);
    if (!state.open || widget._overlayIntent === false) {
        hideAnchoredOverlay(widget);
        if (state.open) overlayCommand(widget, "close");
        return;
    }
    const opening = !widget._overlayShown;
    if (opening) {
        if (typeof widget.showPopover !== "function") return;
        widget.showPopover();
        widget._overlayShown = true;
        anchoredStack.push(widget);
        if (state.kind !== "tooltip") widget._anchor.setAttribute("aria-expanded", "true");
    }
    const viewport = overlayViewport();
    const setStyle = (key, value) => {
        if (widget.style[key] !== value) widget.style[key] = value;
    };
    setStyle("maxWidth", Math.max(0, viewport.width - 16) + "px");
    setStyle("maxHeight", Math.max(0, viewport.height - 16) + "px");
    const size = widget.getBoundingClientRect();
    const result = anchoredPosition(
        widget._anchor.getBoundingClientRect(),
        size,
        viewport,
        state.placement
    );
    setStyle("left", result.x + "px");
    setStyle("top", result.y + "px");
    widget.dataset.placement = result.side;
    if (opening && state.kind !== "tooltip") {
        const stops = state.kind === "menu" ? overlayMenuStops(widget) : modalTabStops(widget);
        const target = widget._overlayLast ? stops.at(-1) : stops[0];
        widget._overlayLast = false;
        if (!focusModalElement(target)) focusModalElement(widget);
    }
}

function scheduleAnchoredOverlays() {
    if (anchoredFrame) return;
    anchoredFrame = requestAnimationFrame(() => {
        anchoredFrame = 0;
        for (const widget of anchoredOverlays) refreshAnchoredOverlay(widget);
    });
}

function overlayMenuStops(widget) {
    return Array.from(widget.querySelectorAll('[role="menuitem"]')).filter(
        button => !button.disabled
    );
}

function handleAnchoredKeys(event) {
    const widget = anchoredStack.at(-1);
    if (!widget || event.defaultPrevented || event.isComposing) return;
    if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        widget._tooltipSuppressed = true;
        closeAnchoredOverlay(widget, widget._overlay.kind !== "tooltip");
        return;
    }
    if (
        widget._overlay.kind !== "menu" ||
        !widget.contains(event.target) ||
        event.ctrlKey ||
        event.altKey ||
        event.metaKey
    )
        return;
    const stops = overlayMenuStops(widget);
    const index = stops.indexOf(document.activeElement);
    let next;
    if (event.key === "ArrowDown") next = (index + 1) % stops.length;
    else if (event.key === "ArrowUp") next = (index + stops.length - 1) % stops.length;
    else if (event.key === "Home") next = 0;
    else if (event.key === "End") next = stops.length - 1;
    else if (event.key === "Tab") {
        // Restore the anchor before the native Tab action chooses the next page stop.
        closeAnchoredOverlay(widget, true);
        return;
    } else return;
    event.preventDefault();
    event.stopPropagation();
    if (stops[next]) stops[next].focus();
}

function startAnchoredManager() {
    if (anchoredScope) return;
    anchoredScope = new AbortController();
    const options = { signal: anchoredScope.signal };
    const root = modalRoot();
    root.addEventListener("keydown", handleAnchoredKeys, { ...options, capture: true });
    root.addEventListener(
        "pointerdown",
        event => {
            const top = anchoredStack.at(-1);
            if (top && !top.contains(event.target) && !top._anchor.contains(event.target))
                closeAnchoredOverlay(top);
        },
        { ...options, capture: true }
    );
    root.addEventListener(
        "focusin",
        event => {
            const top = anchoredStack.at(-1);
            if (
                top &&
                top._overlay.kind !== "tooltip" &&
                !top.contains(event.target) &&
                event.target !== top._anchor
            )
                closeAnchoredOverlay(top);
        },
        options
    );
    window.addEventListener("scroll", scheduleAnchoredOverlays, { ...options, capture: true });
    window.addEventListener("resize", scheduleAnchoredOverlays, options);
    window.visualViewport?.addEventListener("resize", scheduleAnchoredOverlays, options);
    window.visualViewport?.addEventListener("scroll", scheduleAnchoredOverlays, options);
    anchoredObserver = new MutationObserver(scheduleAnchoredOverlays);
    anchoredObserver.observe(root, {
        subtree: true,
        childList: true,
        attributes: true,
        attributeFilter: ["style", "hidden", "disabled", "inert"]
    });
}

function stopAnchoredManager() {
    if (anchoredOverlays.size) return;
    anchoredScope?.abort();
    anchoredScope = null;
    anchoredObserver?.disconnect();
    anchoredObserver = null;
    if (anchoredFrame) cancelAnimationFrame(anchoredFrame);
    anchoredFrame = 0;
}

function detachOverlayAnchor(widget) {
    widget._anchorScope?.abort();
    widget._anchorScope = null;
    clearTimeout(widget._overlayTimer);
    widget._tooltipHover = false;
    widget._tooltipFocus = false;
    widget._tooltipInside = false;
    widget._tooltipSuppressed = false;
    const anchor = widget._anchor;
    if (anchor) {
        if (widget._overlay?.kind === "tooltip") {
            anchor._tooltipDescriptions?.delete(widget.id);
            refreshAccessibleRelationsFor(anchor);
        } else if (anchor.getAttribute("aria-controls") === widget.id) {
            anchor.removeAttribute("aria-controls");
            anchor.removeAttribute("aria-expanded");
            anchor.removeAttribute("aria-haspopup");
        }
    }
    widget._overlayResize?.disconnect();
    widget._overlayResize = null;
}

function attachOverlayAnchor(widget) {
    detachOverlayAnchor(widget);
    const anchor = widget._anchor;
    if (!anchor || widget._treePaused || !widget.isConnected) return;
    const tooltip = widget._overlay.kind === "tooltip";
    widget.id ||= "widgets-overlay-" + widget._id;
    if (tooltip) {
        anchor._accessibleReferences ??= {};
        anchor._tooltipDescriptions ??= new Set();
        anchor._tooltipDescriptions.add(widget.id);
        accessibilityConsumers.add(anchor);
        refreshAccessibleRelationsFor(anchor);
    } else {
        anchor.setAttribute("aria-controls", widget.id);
        anchor.setAttribute("aria-haspopup", widget._overlay.kind === "menu" ? "menu" : "dialog");
        anchor.setAttribute("aria-expanded", String(!!widget._overlayShown));
    }
    widget._anchorScope = new AbortController();
    const options = { signal: widget._anchorScope.signal };
    const request = () => {
        widget._overlayIntent = true;
        if (!widget._overlay.open) overlayCommand(widget, "open");
        else refreshAnchoredOverlay(widget);
    };
    const tooltipIntent = () => {
        clearTimeout(widget._overlayTimer);
        const wanted = widget._tooltipHover || widget._tooltipFocus || widget._tooltipInside;
        if (!wanted) widget._tooltipSuppressed = false;
        widget._overlayTimer = setTimeout(
            () => {
                if (wanted && !widget._tooltipSuppressed) request();
                else if (!wanted) closeAnchoredOverlay(widget);
            },
            wanted
                ? widget._tooltipFocus
                    ? 0
                    : widget._overlay.showDelay
                : widget._overlay.hideDelay
        );
    };
    if (tooltip) {
        anchor.addEventListener(
            "pointerenter",
            event => {
                if (event.pointerType === "touch") return;
                widget._tooltipHover = true;
                tooltipIntent();
            },
            options
        );
        anchor.addEventListener(
            "pointerleave",
            () => {
                widget._tooltipHover = false;
                tooltipIntent();
            },
            options
        );
        anchor.addEventListener(
            "focus",
            () => {
                widget._tooltipFocus = true;
                tooltipIntent();
            },
            options
        );
        anchor.addEventListener(
            "blur",
            () => {
                widget._tooltipFocus = false;
                tooltipIntent();
            },
            options
        );
        widget.addEventListener(
            "pointerenter",
            () => {
                widget._tooltipInside = true;
                tooltipIntent();
            },
            options
        );
        widget.addEventListener(
            "pointerleave",
            () => {
                widget._tooltipInside = false;
                tooltipIntent();
            },
            options
        );
    } else {
        anchor.addEventListener(
            "click",
            () => {
                if (widget._overlayShown) closeAnchoredOverlay(widget, true);
                else request();
            },
            options
        );
        anchor.addEventListener(
            "keydown",
            event => {
                if (
                    widget._overlay.kind !== "menu" ||
                    event.altKey ||
                    event.ctrlKey ||
                    event.metaKey ||
                    !["ArrowDown", "ArrowUp"].includes(event.key)
                )
                    return;
                event.preventDefault();
                widget._overlayLast = event.key === "ArrowUp";
                request();
            },
            options
        );
    }
    widget._overlayResize = new ResizeObserver(scheduleAnchoredOverlays);
    widget._overlayResize.observe(anchor);
    widget._overlayResize.observe(widget);
}

function createAnchoredOverlay(kind) {
    const widget = document.createElement("div");
    widget.className = "widgets-anchored-overlay widgets-anchored-" + kind;
    widget.setAttribute("popover", "manual");
    widget.setAttribute("role", kind === "popover" ? "dialog" : kind);
    if (kind !== "tooltip") widget.tabIndex = -1;
    widget.style.width = widget._defaultWidth = "max-content";
    widget.style.height = widget._defaultHeight = "auto";
    widget._onAttached = () => {
        anchoredOverlays.add(widget);
        startAnchoredManager();
        if (widget._overlay) attachOverlayAnchor(widget);
        scheduleAnchoredOverlays();
    };
    const stop = () => {
        widget._overlayIntent = false;
        hideAnchoredOverlay(widget);
        detachOverlayAnchor(widget);
        anchoredOverlays.delete(widget);
        stopAnchoredManager();
    };
    widget._onDetached = stop;
    widget._onDisposed = () => {
        stop();
        widget._overlay = null;
        widget._anchor = null;
    };
    listenWidget(widget, "click", event => {
        const button = event.target.closest('[role="menuitem"]');
        if (
            widget._overlay?.kind !== "menu" ||
            !button ||
            !widget.contains(button) ||
            button.disabled
        )
            return;
        if (overlayCommand(widget, "choose", button.dataset.key)) {
            widget._overlayIntent = false;
            hideAnchoredOverlay(widget, true);
        }
    });
    return widget;
}

function setAnchoredOverlay(data) {
    const widget = widgets[data.widget];
    const state = data.snapshot;
    const anchor =
        widgets[state?.anchor] ||
        (widget?._overlay?.anchor === state?.anchor ? widget._anchor : null);
    const content = widgets[state?.content];
    if (
        !widget?.classList.contains("widgets-anchored-overlay") ||
        !state ||
        !anchor ||
        !content ||
        content.parentElement !== widget ||
        widget.contains(anchor) ||
        !["menu", "popover", "tooltip"].includes(state.kind) ||
        !widget.classList.contains("widgets-anchored-" + state.kind) ||
        (state.kind !== "tooltip" && anchor.tagName !== "BUTTON") ||
        !["top", "bottom", "left", "right"].includes(state.placement) ||
        typeof state.open !== "boolean" ||
        !Number.isInteger(state.revision) ||
        state.revision < 0 ||
        ![state.showDelay, state.hideDelay].every(
            value => Number.isInteger(value) && value >= 0 && value <= 5000
        ) ||
        !Array.isArray(state.items) ||
        state.items.length > 256
    )
        return false;
    const keys = new Set();
    for (const item of state.items) {
        if (
            typeof item.key !== "string" ||
            !item.key.trim() ||
            item.key.length > 128 ||
            keys.has(item.key) ||
            typeof item.label !== "string" ||
            !item.label.trim() ||
            typeof item.enabled !== "boolean"
        )
            return false;
        keys.add(item.key);
    }
    const previous = widget._overlay;
    if (!previous || previous.revision !== state.revision) widget._overlayPending = false;
    if (state.open && !previous?.open && widget._overlayIntent !== false)
        widget._overlayIntent = true;
    if (!state.open) widget._overlayIntent = undefined;
    widget._overlay = state;
    const changedAnchor = widget._anchor !== anchor;
    widget._anchor = anchor;
    if (state.kind === "menu") {
        const focused = document.activeElement?.dataset.key;
        const nodes = new Map(Array.from(content.children).map(node => [node.dataset.key, node]));
        for (const item of state.items) {
            const button = nodes.get(item.key) || document.createElement("button");
            button.type = "button";
            button.setAttribute("role", "menuitem");
            button.dataset.key = item.key;
            button.textContent = item.label;
            button.disabled = !item.enabled;
            button.tabIndex = -1;
            content.append(button);
            nodes.delete(item.key);
        }
        for (const node of nodes.values()) node.remove();
        if (widget._overlayShown && focused) {
            const target =
                overlayMenuStops(widget).find(node => node.dataset.key === focused) ||
                overlayMenuStops(widget)[0];
            if (target) target.focus();
            else widget.focus();
        }
    }
    if (!state.open && widget._overlayShown) hideAnchoredOverlay(widget, state.kind !== "tooltip");
    if (!widget._anchorScope || changedAnchor) attachOverlayAnchor(widget);
    refreshAnchoredOverlay(widget);
    return true;
}
