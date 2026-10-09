/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createSortableSection() {
    const widget = widgetsLibrary.section();
    widget._revision = 0;
    widget._pendingReorder = false;
    widget._animationDuration = 0;
    widget.dataset.sortable = "true";
    let gesture = null;
    const animations = new Map();
    initGestureClicks(widget);
    widget._layoutChild = child => {
        child.style.touchAction = "none";
        child.style.userSelect = "none";
        if (!child.hasAttribute("tabindex")) child.tabIndex = 0;
        child.setAttribute("aria-keyshortcuts", "Alt+ArrowLeft Alt+ArrowRight");
    };
    const interactive = target =>
        target.closest(
            "button,a,input,select,textarea,[contenteditable]:not([contenteditable=false]),[data-sortable],[data-zoom]"
        );
    function directChild(target) {
        while (target && target.parentElement !== widget) target = target.parentElement;
        return target;
    }
    function positions() {
        return new Map(
            Array.from(widget.children, child => [child, child.getBoundingClientRect()])
        );
    }
    function stopAnimations() {
        for (const entry of animations.values()) {
            entry.animation.cancel();
            entry.restore();
        }
        animations.clear();
    }
    widget._stopSortAnimations = stopAnimations;
    // FLIP: sample visual positions, change layout once, then animate back from the old positions.
    function reflow(change, raisedChild = null) {
        const before = positions();
        stopAnimations();
        change();
        const duration = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches
            ? 0
            : widget._animationDuration;
        if (!duration) return;
        for (const child of widget.children) {
            const old = before.get(child);
            if (!old || !child.animate) continue;
            const rect = child.getBoundingClientRect();
            const dx = old.left - rect.left;
            const dy = old.top - rect.top;
            if (Math.abs(dx) < 0.1 && Math.abs(dy) < 0.1) continue;
            const transform = getComputedStyle(child).transform;
            const saved = { position: child.style.position, zIndex: child.style.zIndex };
            if (child === raisedChild) {
                child.style.position = "relative";
                child.style.zIndex = "1000";
            }
            const restore = () => {
                if (child === raisedChild) Object.assign(child.style, saved);
            };
            const animation = child.animate(
                [
                    {
                        transform: `translate(${dx}px, ${dy}px) ${transform === "none" ? "" : transform}`
                    },
                    { transform }
                ],
                { duration, easing: "cubic-bezier(0.2, 0, 0, 1)" }
            );
            animations.set(child, { animation, restore });
            animation.onfinish = () => {
                if (animations.get(child)?.animation === animation) {
                    animations.delete(child);
                    restore();
                }
            };
        }
    }
    function clearGesture() {
        if (!gesture) return null;
        const current = gesture;
        gesture = null;
        Object.assign(current.child.style, current.style, { transition: "none" });
        current.child.getBoundingClientRect();
        current.child.style.transition = current.style.transition;
        if (current.moved) widget._suppressGestureClick();
        releaseGesturePointer(widget, current.id);
        return current;
    }
    function reportMove(child) {
        widget._pendingReorder = true;
        sendEventToServer(widget, "reorder", {
            revision: widget._revision++,
            child: child._id,
            before: child.nextElementSibling?._id || ""
        });
    }
    function finish(cancelled) {
        if (!gesture) return;
        const current = gesture;
        reflow(() => {
            clearGesture();
            if (!cancelled && current.moved && current.before !== current.child) {
                widget.insertBefore(current.child, current.before);
            }
        }, current.child);
        if (
            !cancelled &&
            current.moved &&
            Array.from(widget.children).some((child, index) => child !== current.order[index])
        ) {
            reportMove(current.child);
        }
    }
    widget._cancelGesture = () => finish(true);
    widget._applySortOrder = children => {
        // An acknowledgement must not interrupt the drop animation already running locally.
        if (!gesture && children.every((child, index) => child === widget.children[index])) return;
        const focused = document.activeElement;
        reflow(() => {
            clearGesture();
            for (const child of children) widget.appendChild(child);
            if (focused && widget.contains(focused)) focused.focus({ preventScroll: true });
        });
    };
    widget._onDetached = () => {
        clearGesture();
        stopAnimations();
    };
    widget.addEventListener("pointerdown", event => {
        if (gesture) {
            finish(true);
            return;
        }
        if (widget._pendingReorder || event.button !== 0 || event.isPrimary === false) return;
        const control = interactive(event.target);
        if (control && control !== widget) return;
        const child = directChild(event.target);
        if (!child) return;
        stopAnimations();
        const rect = child.getBoundingClientRect();
        gesture = {
            id: event.pointerId,
            child,
            x: event.clientX,
            y: event.clientY,
            offsetX: event.clientX - rect.left,
            offsetY: event.clientY - rect.top,
            dx: 0,
            dy: 0,
            moved: false,
            before: child,
            order: Array.from(widget.children),
            transform: getComputedStyle(child).transform,
            style: {
                transform: child.style.transform,
                position: child.style.position,
                zIndex: child.style.zIndex,
                transition: child.style.transition
            }
        };
        widget.setPointerCapture?.(event.pointerId);
    });
    function move(event) {
        if (!gesture || event.pointerId !== gesture.id) return;
        if (!gesture.moved && Math.hypot(event.clientX - gesture.x, event.clientY - gesture.y) < 6)
            return;
        const current = gesture;
        const child = current.child;
        const rect = child.getBoundingClientRect();
        const base = {
            left: rect.left - current.dx,
            top: rect.top - current.dy,
            right: rect.right - current.dx,
            bottom: rect.bottom - current.dy,
            width: rect.width
        };
        current.moved = true;
        current.dx = event.clientX - current.offsetX - base.left;
        current.dy = event.clientY - current.offsetY - base.top;
        Object.assign(child.style, {
            position: "relative",
            zIndex: "1000",
            transition: "none",
            transform: `translate(${current.dx}px, ${current.dy}px) ${current.transform === "none" ? "" : current.transform}`
        });
        event.preventDefault();
        // Keep every layout slot intact until release, including the dragged card's own slot.
        let nearest = child;
        let targetRect = base;
        let distance = Infinity;
        for (const candidate of widget.children) {
            const box = candidate === child ? base : candidate.getBoundingClientRect();
            const dx = Math.max(box.left - event.clientX, 0, event.clientX - box.right);
            const dy = Math.max(box.top - event.clientY, 0, event.clientY - box.bottom);
            const score = dx * dx + dy * dy;
            if (score < distance) {
                distance = score;
                nearest = candidate;
                targetRect = box;
            }
        }
        if (nearest === child) {
            current.before = child;
            return;
        }
        const after =
            event.clientY > targetRect.bottom ||
            (event.clientY >= targetRect.top &&
                event.clientX > targetRect.left + targetRect.width / 2);
        current.before = after ? nearest.nextElementSibling : nearest;
    }
    widget.addEventListener("pointermove", move);
    widget.addEventListener("pointerup", event => {
        if (event.pointerId === gesture?.id) {
            move(event);
            finish(false);
        }
    });
    for (const type of ["pointercancel", "lostpointercapture"]) {
        widget.addEventListener(type, event => {
            if (event.pointerId === gesture?.id) finish(true);
        });
    }
    widget.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            finish(true);
            return;
        }
        if (!event.altKey || !["ArrowLeft", "ArrowRight"].includes(event.key)) return;
        const child = directChild(event.target);
        if (!child || event.target !== child || gesture || widget._pendingReorder) return;
        const next =
            event.key === "ArrowLeft" ? child.previousElementSibling : child.nextElementSibling;
        if (!next) return;
        event.preventDefault();
        reflow(() => {
            widget.insertBefore(child, event.key === "ArrowLeft" ? next : next.nextElementSibling);
            child.focus({ preventScroll: true });
        });
        reportMove(child);
    });
    return widget;
}

function setAnimationDuration(data) {
    const widget = widgets[data.widget];
    if (
        !widget?._applySortOrder ||
        !Number.isInteger(data["animation duration"]) ||
        data["animation duration"] < 0
    )
        return false;
    widget._animationDuration = data["animation duration"];
    if (!data["animation duration"]) widget._stopSortAnimations();
    return true;
}

function setChildOrder(data) {
    const widget = widgets[data.widget];
    if (
        !widget?._cancelGesture ||
        !Array.isArray(data.children) ||
        !Number.isInteger(data.revision) ||
        data.revision < 0
    )
        return false;
    const children = data.children.map(id => widgets[id]);
    if (
        children.length !== widget.children.length ||
        new Set(children).size !== children.length ||
        children.some(child => !child || child.parentElement !== widget)
    )
        return false;
    widget._applySortOrder(children);
    widget._revision = data.revision;
    widget._pendingReorder = false;
    return true;
}
