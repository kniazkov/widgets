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
        // Auto margins center a shrink-wrapped panel; explicit Widget widths/heights
        // still override these defaults through setWidth/setHeight.
        widget.style.width = widget._defaultWidth = "fit-content";
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
                    widget._closeOnOutsideClick &&
                    event.target === backdrop &&
                    event.button === 0 &&
                    activate
                ) {
                    sendEventToServer(widget, "dismiss", {});
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
