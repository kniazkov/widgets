/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Suggestions are view state; the text and source list remain server-backed properties.
function createSuggestionField() {
    const widget = createInputField();
    // WebKit client rects use the visual viewport origin (including on iOS).
    // Keep clipping bounds in that same space; adding viewport offsets again
    // would incorrectly hide a visible field after the keyboard pans the page.
    const visualClientCoordinates =
        window.CSS?.supports?.("-webkit-backdrop-filter", "none") === true;
    widget.setAttribute("role", "combobox");
    widget.setAttribute("aria-autocomplete", "list");
    widget.setAttribute("aria-expanded", "false");
    widget.autocomplete = "off";
    widget._suggestions = [];
    widget._suggestionSeparator = "";
    let list = null;
    let active = -1;
    let observer = null;
    let composing = false;
    let touch = null;
    let positionFrame = null;

    function close() {
        touch = null;
        if (positionFrame !== null) {
            window.cancelAnimationFrame(positionFrame);
            positionFrame = null;
        }
        if (!list) return;
        list.remove();
        list = null;
        active = -1;
        widget.setAttribute("aria-expanded", "false");
        widget.removeAttribute("aria-controls");
        widget.removeAttribute("aria-activedescendant");
        document.removeEventListener("pointerdown", outside, true);
        window.removeEventListener("resize", position);
        document.removeEventListener("scroll", position, true);
        window.visualViewport?.removeEventListener("resize", position);
        window.visualViewport?.removeEventListener("scroll", position);
        observer?.disconnect();
        observer = null;
    }

    function outside(event) {
        if (event.target !== widget && !list?.contains(event.target)) close();
    }

    function position() {
        if (!list) return;
        const rect = widget.getBoundingClientRect();
        const viewport = window.visualViewport;
        const top = (visualClientCoordinates ? 0 : (viewport?.offsetTop ?? 0)) + 4;
        const left = (visualClientCoordinates ? 0 : (viewport?.offsetLeft ?? 0)) + 4;
        const bottom = top + (viewport?.height ?? window.innerHeight) - 8;
        const right = left + (viewport?.width ?? window.innerWidth) - 8;
        const below = Math.max(0, bottom - rect.bottom - 4);
        const above = Math.max(0, rect.top - top - 4);
        if (
            rect.bottom <= top ||
            rect.top >= bottom ||
            rect.right <= left ||
            rect.left >= right ||
            right <= left ||
            Math.max(above, below) <= 2
        ) {
            setListStyle("visibility", "hidden");
            return;
        }
        const upward = below < 160 && above > below;
        const width = Math.min(rect.width, right - left);
        setListStyle("width", width + "px");
        setListStyle("maxHeight", Math.min(240, upward ? above : below) + "px");
        const bounds = list.getBoundingClientRect();
        const x = Math.max(left, Math.min(rect.left, right - width));
        const y = upward ? rect.top - 4 - bounds.height : rect.bottom + 4;
        // Measure the rendered popup too: mobile fixed/top-layer positioning can
        // shift independently while the keyboard pans the visual viewport.
        if (Math.abs(x - bounds.left) > 0.5) {
            setListStyle("left", parseFloat(list.style.left) + x - bounds.left + "px");
        }
        if (Math.abs(y - bounds.top) > 0.5) {
            setListStyle("top", parseFloat(list.style.top) + y - bounds.top + "px");
        }
        setListStyle("visibility", "visible");
    }

    function setListStyle(name, value) {
        if (list.style[name] !== value) list.style[name] = value;
    }

    function trackPosition() {
        positionFrame = null;
        if (!list) return;
        if (!widget.isConnected) {
            close();
            return;
        }
        // Focus scrolling and layout changes can finish after the last viewport
        // event. Track only the open list, without rewriting unchanged styles.
        position();
        positionFrame = window.requestAnimationFrame(trackPosition);
    }

    function highlight(index) {
        if (!list) return;
        active = index;
        Array.from(list.children).forEach((option, i) => {
            option.setAttribute("aria-selected", String(i === active));
        });
        const option = list.children[active];
        if (option) {
            widget.setAttribute("aria-activedescendant", option.id);
            option.scrollIntoView?.({ block: "nearest" });
        } else {
            widget.removeAttribute("aria-activedescendant");
        }
    }

    // A selection spanning separators is deliberately not replaced by a suggestion.
    function token() {
        const text = widget.value;
        const separator = widget._suggestionSeparator;
        if (!separator) return { start: 0, end: text.length, query: text };
        const from = widget.selectionStart ?? text.length;
        const to = widget.selectionEnd ?? from;
        let start = 0;
        let index = 0;
        while (true) {
            const boundary = text.indexOf(separator, start);
            const end = boundary < 0 ? text.length : boundary;
            if (from <= end) {
                if (to > end) return null;
                const raw = text.slice(start, end);
                const leading = raw.match(/^\s*/)[0].length;
                const trailing = raw.trim() ? raw.match(/\s*$/)[0].length : 0;
                return { start: start + leading, end: end - trailing, query: raw.trim(), index };
            }
            if (boundary < 0 || from < end + separator.length) return null;
            start = end + separator.length;
            index++;
        }
    }

    function keyOf(value) {
        return value.trim().toLowerCase();
    }

    function otherValues(current) {
        return new Set(
            widget._suggestionSeparator
                ? widget.value
                      .split(widget._suggestionSeparator)
                      .filter((value, index) => index !== current.index)
                      .map(keyOf)
                : []
        );
    }

    function removeDuplicates() {
        if (!widget._suggestionSeparator || widget.disabled || composing) return;
        const seen = new Set();
        const value = widget.value
            .split(widget._suggestionSeparator)
            .filter(token => {
                const key = keyOf(token);
                // Empty tokens remain editable; this is not required-value validation.
                if (!key) return true;
                if (seen.has(key)) return false;
                seen.add(key);
                return true;
            })
            .join(widget._suggestionSeparator);
        if (value !== widget.value) {
            widget.value = value;
            widget.dispatchEvent(new Event("input", { bubbles: true }));
        }
    }

    function select(value) {
        if (widget.disabled) return;
        const current = token();
        if (!current || composing || otherValues(current).has(keyOf(value))) return;
        widget.value =
            widget.value.slice(0, current.start) + value + widget.value.slice(current.end);
        const caret = current.start + value.length;
        // Focus synchronously within the user gesture so mobile keyboards stay available.
        widget.focus({ preventScroll: true });
        widget.setSelectionRange(caret, caret);
        close();
        // Use the ordinary text-input pipeline, including delayed-echo protection.
        widget.dispatchEvent(new Event("input", { bubbles: true }));
        close();
        widget.setSelectionRange(caret, caret);
        // Selection alone does not reliably reveal a long value on mobile Safari.
        // Leave middle-token scrolling to the browser's caret positioning.
        if (caret === widget.value.length) widget.scrollLeft = widget.scrollWidth;
    }

    function render() {
        close();
        if (widget.disabled || document.activeElement !== widget || composing) return;
        const current = token();
        if (!current) return;
        const query = current.query.toLowerCase();
        const used = otherValues(current);
        const values = [...new Set(widget._suggestions)].filter(
            value =>
                value.trim() &&
                !used.has(keyOf(value)) &&
                value.toLowerCase().includes(query) &&
                (!widget._suggestionSeparator || !value.includes(widget._suggestionSeparator))
        );
        if (!values.length) return;
        list = document.createElement("div");
        list.className = "suggestion-list";
        list.style.top = "0px";
        list.style.left = "0px";
        list.id = "suggestions-" + widget._id;
        list.setAttribute("role", "listbox");
        const style = getComputedStyle(widget);
        list.style.font = style.font;
        list.style.color = style.color;
        list.style.backgroundColor = style.backgroundColor;
        list.style.borderColor = style.borderColor;
        list.style.borderRadius = style.borderRadius;
        // Safari may blur the input before dispatching a compatibility click. Keep
        // the list alive for the whole touch gesture and select on touchend instead.
        // Unlike pointerup, cancelling touchend also suppresses that later click.
        list.addEventListener(
            "touchstart",
            event => {
                if (event.touches.length !== 1) {
                    if (touch) touch.cancelled = true;
                    return;
                }
                const option = event.target.closest('[role="option"]');
                if (!option || option.parentNode !== list) return;
                const point = event.touches[0];
                touch = {
                    id: point.identifier,
                    x: point.clientX,
                    y: point.clientY,
                    option,
                    cancelled: false
                };
            },
            { passive: true }
        );
        list.addEventListener(
            "touchmove",
            event => {
                if (!touch) return;
                const point = Array.from(event.touches).find(
                    point => point.identifier === touch.id
                );
                if (!point || Math.hypot(point.clientX - touch.x, point.clientY - touch.y) > 8) {
                    touch.cancelled = true;
                }
            },
            { passive: true }
        );
        list.addEventListener("pointercancel", () => {
            if (touch) touch.cancelled = true;
        });
        list.addEventListener("touchcancel", () => {
            touch = null;
            if (document.activeElement !== widget) {
                close();
                removeDuplicates();
            }
        });
        list.addEventListener(
            "touchend",
            event => {
                if (!touch) return;
                const point = Array.from(event.changedTouches).find(
                    point => point.identifier === touch.id
                );
                if (!point) return;
                const gesture = touch;
                touch = null;
                // Never allow a delayed click to reach a removed option or the input
                // underneath it (which would reopen the list and move the caret).
                event.preventDefault();
                if (
                    !gesture.cancelled &&
                    event.touches.length === 0 &&
                    Math.hypot(point.clientX - gesture.x, point.clientY - gesture.y) <= 8
                ) {
                    select(gesture.option.textContent);
                } else if (document.activeElement !== widget) {
                    close();
                    removeDuplicates();
                }
            },
            { passive: false }
        );
        values.forEach((value, index) => {
            const option = document.createElement("div");
            option.id = list.id + "-" + index;
            option.setAttribute("role", "option");
            option.setAttribute("aria-selected", "false");
            option.textContent = value;
            // Keep focus and the on-screen keyboard on the editable field.
            option.addEventListener("pointerdown", event => event.preventDefault());
            option.addEventListener("mousedown", event => event.preventDefault());
            // Mouse and assistive-technology activation still use click.
            option.addEventListener("click", event => {
                event.preventDefault();
                if (option.isConnected) select(value);
            });
            list.appendChild(option);
        });
        document.body.appendChild(list);
        // The top layer also works when the field is inside a modal popup.
        if (typeof list.showPopover === "function") {
            list.setAttribute("popover", "manual");
            list.showPopover();
        }
        widget.setAttribute("aria-controls", list.id);
        widget.setAttribute("aria-expanded", "true");
        position();
        positionFrame = window.requestAnimationFrame(trackPosition);
        document.addEventListener("pointerdown", outside, true);
        window.addEventListener("resize", position);
        document.addEventListener("scroll", position, true);
        window.visualViewport?.addEventListener("resize", position);
        window.visualViewport?.addEventListener("scroll", position);
        // Parent removal does not invoke each descendant's detach hook.
        observer = new MutationObserver(() => {
            if (!widget.isConnected) close();
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    widget._setSuggestions = values => {
        widget._suggestions = values.slice();
        render();
    };
    widget._setSuggestionSeparator = value => {
        widget._suggestionSeparator = value;
        render();
    };
    widget.addEventListener("keyup", event => {
        if (["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) render();
    });
    widget._closeSuggestions = close;
    widget._onDetached = close;
    widget.addEventListener("focus", render);
    widget.addEventListener("click", render);
    // A repeated tap need not focus again or produce a compatibility click.
    // Never focus programmatically here: scrolling or leaving the field must
    // not steal focus or reopen the software keyboard.
    widget.addEventListener("pointerup", event => {
        if (event.pointerType === "touch" && document.activeElement === widget) render();
    });
    widget.addEventListener("input", render);
    widget.addEventListener("blur", () => {
        if (touch) return;
        close();
        removeDuplicates();
    });
    widget.addEventListener("compositionstart", () => {
        composing = true;
        close();
    });
    widget.addEventListener("compositionend", () => {
        composing = false;
        if (document.activeElement !== widget) removeDuplicates();
        render();
    });
    widget.addEventListener("keydown", event => {
        if (event.isComposing || composing) return;
        if (event.key === "Escape" || event.key === "Tab") {
            if (list && event.key === "Escape") {
                event.preventDefault();
                event.stopPropagation();
            }
            close();
        } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            if (!list) render();
            if (list) {
                const count = list.children.length;
                highlight(
                    event.key === "ArrowDown"
                        ? (active + 1) % count
                        : active < 0
                          ? count - 1
                          : (active + count - 1) % count
                );
            }
        } else if (event.key === "Enter" && list && active >= 0) {
            event.preventDefault();
            select(list.children[active].textContent);
        }
    });
    return widget;
}

function setSuggestionSeparator(data) {
    const widget = widgets[data.widget];
    if (!widget?._setSuggestionSeparator || typeof data["suggestion separator"] !== "string")
        return false;
    widget._setSuggestionSeparator(data["suggestion separator"]);
    return true;
}

function setSuggestions(data) {
    const widget = widgets[data.widget];
    if (
        widget?._setSuggestions &&
        Array.isArray(data.suggestions) &&
        data.suggestions.every(value => typeof value === "string")
    ) {
        widget._setSuggestions(data.suggestions);
        return true;
    }
    return false;
}
