/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Popups are fixed to the viewport. A modal popup owns an adjacent full-screen backdrop.
function createPopup(modal) {
    const widget = document.createElement("div");
    widget.className = modal ? "modal-popup" : "popup";
    widget.style.position = "fixed";
    widget.style.zIndex = modal ? "1002" : "1001";
    widget._horzAlignment = "center";
    widget._vertAlignment = "middle";
    widget._refreshPosition = function () {
        if (modal) {
            // A transformed modal would turn any nested fixed backdrop into a panel-sized
            // backdrop. Center with fixed insets and auto margins instead.
            const left = widget._horzAlignment === "left";
            const right = widget._horzAlignment === "right";
            const top = widget._vertAlignment === "top";
            const bottom = widget._vertAlignment === "bottom";
            widget.style.left = right ? "auto" : "0px";
            widget.style.right = left ? "auto" : "0px";
            widget.style.top = bottom ? "auto" : "0px";
            widget.style.bottom = top ? "auto" : "0px";
            widget.style.marginLeft = widget.style.marginRight = left || right ? "0px" : "auto";
            widget.style.marginTop = widget.style.marginBottom = top || bottom ? "0px" : "auto";
            return;
        }
        let translateX = "0";
        let translateY = "0";
        widget.style.left = "";
        widget.style.right = "";
        widget.style.top = "";
        widget.style.bottom = "";

        switch (widget._horzAlignment) {
            case "left":
                widget.style.left = "0px";
                break;
            case "right":
                widget.style.right = "0px";
                break;
            default:
                widget.style.left = "50%";
                translateX = "-50%";
                break;
        }
        switch (widget._vertAlignment) {
            case "top":
                widget.style.top = "0px";
                break;
            case "bottom":
                widget.style.bottom = "0px";
                break;
            default:
                widget.style.top = "50%";
                translateY = "-50%";
                break;
        }
        widget.style.transform = "translate(" + translateX + ", " + translateY + ")";
    };
    widget._setHorzAlignment = function (value) {
        widget._horzAlignment = value;
        widget._refreshPosition();
    };
    widget._setVertAlignment = function (value) {
        widget._vertAlignment = value;
        widget._refreshPosition();
    };
    widget._refreshPosition();

    if (modal) {
        widget.setAttribute("role", "dialog");
        widget.setAttribute("aria-label", "Dialog");
        widget.tabIndex = -1;
        // Auto margins center a shrink-wrapped panel; explicit Widget widths/heights
        // still override these defaults through setWidth/setHeight.
        widget.style.width = widget._defaultWidth = "fit-content";
        widget.setAttribute("data-intrinsic-width", "");
        widget.style.height = widget._defaultHeight = "fit-content";
        const backdrop = document.createElement("div");
        backdrop.className = "popup-backdrop";
        backdrop.style.position = "fixed";
        backdrop.style.inset = "0px";
        // Equal stacking levels and pair order put a later backdrop over every
        // earlier panel in the same stacking context.
        backdrop.style.zIndex = widget.style.zIndex;
        backdrop._isWidgetBackdrop = true;
        backdrop.setAttribute("aria-hidden", "true");
        widget._backdrop = backdrop;
        widget._closeOnOutsideClick = false;
        widget._closeOnEscape = false;
        let pressedOutside = false;
        listenWidget(
            widget,
            "pointerdown",
            event => {
                pressedOutside =
                    event.target === backdrop && event.button === 0 && event.isPrimary !== false;
                event.stopPropagation();
            },
            false,
            backdrop
        );
        // Clicking the backdrop must not transfer focus to the document body.
        listenWidget(widget, "mousedown", event => event.preventDefault(), false, backdrop);
        listenWidget(
            widget,
            "pointercancel",
            () => {
                pressedOutside = false;
            },
            false,
            backdrop
        );
        listenWidget(
            widget,
            "click",
            event => {
                event.preventDefault();
                event.stopPropagation();
                const activate = pressedOutside || event.detail === 0;
                pressedOutside = false;
                if (
                    activeModal === widget &&
                    widget._closeOnOutsideClick &&
                    event.target === backdrop &&
                    event.button === 0 &&
                    activate
                ) {
                    sendEventToServer(widget, "dismiss", {});
                } else if (activeModal === widget && !widget.contains(document.activeElement)) {
                    focusModal(widget);
                }
            },
            false,
            backdrop
        );
        widget._onAttached = function () {
            if (widget.parentNode) {
                widget.parentNode.insertBefore(backdrop, widget);
            }
        };
        widget._setBackdropHidden = function (hidden) {
            if (hidden) pressedOutside = false;
            backdrop.style.display = hidden ? "none" : "";
        };
        widget._onDetached = function () {
            pressedOutside = false;
            backdrop.remove();
        };
    }
    return widget;
}

function setBackdropColor(data) {
    const widget = widgets[data.widget];
    const rgb = data["backdrop color"];
    if (widget && widget._backdrop && rgb !== null && typeof rgb == "object") {
        const color = composeColor(rgb);
        widget._backdrop.style.backgroundColor = color;
        log('The backdrop color "' + color + '" has been set to the widget ' + data.widget + ".");
        return true;
    }
    return false;
}

function setCloseOnOutsideClick(data) {
    const widget = widgets[data.widget];
    const enabled = data["close on outside click"];
    if (!widget?._backdrop || typeof enabled !== "boolean") return false;
    widget._closeOnOutsideClick = enabled;
    return true;
}

let activeModal = null;
let modalListeners = null;
const isolatedModalSiblings = new Map();

function modalRoot() {
    return (typeof pageContext === "undefined" ? null : pageContext?.root) || document.body;
}

function visibleModalElement(element) {
    return (
        element?.isConnected &&
        element.getClientRects().length > 0 &&
        getComputedStyle(element).visibility === "visible"
    );
}

function modalTabStops(modal) {
    const elements = Array.from(
        modal.querySelectorAll(
            "a[href],button,input,select,textarea,[tabindex],summary,[contenteditable=true]"
        )
    ).filter(
        node =>
            node.tabIndex >= 0 &&
            !node.matches(":disabled") &&
            !node.closest("[inert]") &&
            visibleModalElement(node)
    );
    const stops = elements.filter(node => {
        if (node.type !== "radio" || !node.name) return true;
        const peers = elements.filter(
            peer => peer.type === "radio" && peer.name === node.name && peer.form === node.form
        );
        return (peers.find(peer => peer.checked) || peers[0]) === node;
    });
    // Positive tabindex precedes normal DOM order, just as it does outside a dialog.
    return stops.sort(
        (a, b) => (a.tabIndex || Number.MAX_SAFE_INTEGER) - (b.tabIndex || Number.MAX_SAFE_INTEGER)
    );
}

function focusModalElement(element) {
    if (!visibleModalElement(element) || element.matches(":disabled") || element.closest("[inert]"))
        return false;
    element.focus({ preventScroll: true });
    return document.activeElement === element;
}

function focusModal(modal) {
    for (const element of modalTabStops(modal)) {
        if (focusModalElement(element)) return;
    }
    focusModalElement(modal);
}

function handleModalKeys(event) {
    const modal = activeModal;
    if (!modal || event.defaultPrevented || event.isComposing) return;
    if (event.key === "Escape") {
        // A child interaction (for example a suggestions list) may consume Escape first.
        event.preventDefault();
        event.stopPropagation();
        if (modal._closeOnEscape && !event.repeat)
            sendEventToServer(modal, "dismiss by escape", {});
        return;
    }
    if (event.key !== "Tab" || event.ctrlKey || event.altKey || event.metaKey) return;
    const stops = modalTabStops(modal);
    const index = stops.indexOf(document.activeElement);
    event.preventDefault();
    const next = event.shiftKey ? (index <= 0 ? stops.length - 1 : index - 1) : index + 1;
    if (!focusModalElement(stops[next % stops.length])) focusModalElement(modal);
}

function refreshModalStack(activation = null) {
    const root = modalRoot();
    const modals = Array.from(root.querySelectorAll(".modal-popup")).filter(
        node => widgets[node._id] === node && !node._treePaused && visibleModalElement(node)
    );
    const top = modals.at(-1) || null;
    const previous = activeModal;
    const before = document.activeElement;
    activeModal = top;
    const isolated = new Set();
    // A nested modal keeps its ancestors alive. Only their off-path branches become inert.
    for (let node = top; node && node !== root && node.parentElement; node = node.parentElement) {
        for (const sibling of node.parentElement.children) {
            if (sibling !== node && sibling !== top._backdrop) isolated.add(sibling);
        }
    }
    for (const [node, wasInert] of isolatedModalSiblings) {
        if (!isolated.has(node)) {
            node.inert = wasInert;
            isolatedModalSiblings.delete(node);
        }
    }
    for (const node of isolated) {
        if (!isolatedModalSiblings.has(node)) isolatedModalSiblings.set(node, node.inert);
        node.inert = true;
    }
    for (const modal of modals) {
        // Global ARIA attributes would defeat the ancestor's presentation role.
        const coveredAncestor = modal !== top && modal.contains(top);
        modal.setAttribute("role", coveredAncestor ? "presentation" : "dialog");
        if (modal === top) modal.setAttribute("aria-modal", "true");
        else modal.removeAttribute("aria-modal");
        if (coveredAncestor) modal.removeAttribute("aria-label");
        else modal.setAttribute("aria-label", modal._modalAccessibleName || "Dialog");
        refreshAccessibleRelationsFor(modal);
    }
    if (previous && !modals.includes(previous)) previous.removeAttribute("aria-modal");
    if (top && !modalListeners) {
        modalListeners = new AbortController();
        const { signal } = modalListeners;
        root.addEventListener("keydown", handleModalKeys, { signal });
        root.addEventListener(
            "focusin",
            event => {
                if (activeModal && !activeModal.contains(event.target)) focusModal(activeModal);
            },
            { signal }
        );
    } else if (!top && modalListeners) {
        modalListeners.abort();
        modalListeners = null;
    }
    if (!root.isConnected) return;
    if (top && previous !== top && (!previous || modals.includes(previous))) {
        const opener =
            (activation && activation.tabIndex >= 0 && (!previous || previous.contains(activation))
                ? activation
                : null) || (before !== document.body && before !== root ? before : null);
        top._modalReturnFocus ??= opener || null;
        focusModal(top);
    } else if (previous && previous !== top) {
        const opener = previous._modalReturnFocus;
        previous._modalReturnFocus = null;
        if ((!top || top.contains(opener)) && focusModalElement(opener)) {
            opener.classList.add("modal-return-focus");
            opener.addEventListener("blur", () => opener.classList.remove("modal-return-focus"), {
                once: true
            });
            return;
        }
        if (top) focusModal(top);
        else {
            if (!root.hasAttribute("tabindex")) root.tabIndex = -1;
            focusModalElement(root);
        }
    } else if (
        top &&
        (!top.contains(before) ||
            !visibleModalElement(before) ||
            before.matches(":disabled") ||
            before.closest("[inert]"))
    ) {
        focusModal(top);
    }
}

function setCloseOnEscape(data) {
    const widget = widgets[data.widget];
    if (!widget?._backdrop || typeof data["close on escape"] !== "boolean") return false;
    widget._closeOnEscape = data["close on escape"];
    return true;
}

function setModalAccessibleDescription(data) {
    const widget = widgets[data.widget];
    const description = data["accessible description"];
    if (!widget?._backdrop || typeof description !== "string") return false;
    if (description.trim()) {
        if (!widget._modalDescription) {
            const label = document.createElement("span");
            label.hidden = true;
            label._isModalDescription = true;
            label.id = "widgets-dialog-description-" + data.widget;
            widget.appendChild(label);
            widget._modalDescription = label;
        }
        widget._modalDescription.textContent = description.trim();
    } else {
        widget._modalDescription?.remove();
        widget._modalDescription = null;
    }
    refreshAccessibleRelationsFor(widget);
    return true;
}

function setModalAccessibleName(widget, name) {
    widget._modalAccessibleName = name.trim();
    if (widget.getAttribute("role") !== "presentation") {
        widget.setAttribute("aria-label", widget._modalAccessibleName || "Dialog");
    }
    return true;
}
