/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Popups are fixed to the viewport. A modal popup owns an adjacent full-screen backdrop.
function createPopup(modal) {
    const widget = document.createElement("div");
    widget.className = modal ? "modal-popup" : "popup";
    widget.style.position = "fixed";
    widget.style.zIndex = "1001";
    widget._horzAlignment = "center";
    widget._vertAlignment = "middle";
    widget._refreshPosition = function () {
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
        const backdrop = document.createElement("div");
        backdrop.className = "popup-backdrop";
        backdrop.style.position = "fixed";
        backdrop.style.inset = "0px";
        backdrop.style.zIndex = "1000";
        widget._backdrop = backdrop;
        widget._closeOnOutsideClick = false;
        let pressedOutside = false;
        backdrop.addEventListener("pointerdown", event => {
            pressedOutside =
                event.target === backdrop && event.button === 0 && event.isPrimary !== false;
            event.stopPropagation();
        });
        backdrop.addEventListener("pointercancel", () => {
            pressedOutside = false;
        });
        backdrop.addEventListener("click", event => {
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
        });
        widget._onAttached = function () {
            if (widget.parentNode) {
                widget.parentNode.insertBefore(backdrop, widget);
            }
        };
        widget._onDetached = function () {
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
