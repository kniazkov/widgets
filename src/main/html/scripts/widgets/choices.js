/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createTypedSelect(multiple) {
    const widget = document.createElement("select");
    widget.className = "typed-choice-select";
    widget.multiple = multiple;
    if (multiple) widget.size = 4;
    listenWidget(widget, "change", () => {
        const keys = [...widget.selectedOptions].map(option => option.value).filter(Boolean);
        sendEventToServer(widget, "choose keys", { keys });
    });
    widget._applyChoiceState = state => {
        const keys = new Set(state.keys);
        const options = state.options.map(item => {
            const option = new Option(item.label, item.key, false, keys.has(item.key));
            option.disabled = item.disabled;
            return option;
        });
        if (!multiple) {
            options.unshift(new Option(state.placeholder || "—", "", false, !keys.size));
        }
        widget.replaceChildren(...options);
        for (const option of options) option.selected = keys.has(option.value);
        if (!multiple) widget.value = state.keys[0] || "";
        if (multiple) widget.title = state.placeholder;
    };
    return widget;
}

let comboIdentifier = 0;

function createTypedComboBox() {
    const widget = document.createElement("span");
    widget.className = "typed-choice-combo";
    const input = document.createElement("input");
    const popup = document.createElement("div");
    const list = document.createElement("div");
    const status = document.createElement("div");
    const next = document.createElement("button");
    const clear = document.createElement("button");
    const prefix = "typed-combo-" + ++comboIdentifier;
    input.setAttribute("role", "combobox");
    input.setAttribute("aria-autocomplete", "list");
    input.setAttribute("aria-controls", prefix + "-options");
    input.setAttribute("aria-expanded", "false");
    input.autocomplete = "off";
    input.maxLength = 256;
    list.setAttribute("role", "listbox");
    list.setAttribute("aria-label", "Matching choices");
    list.id = prefix + "-options";
    status.setAttribute("role", "status");
    status.setAttribute("aria-atomic", "true");
    next.type = "button";
    next.textContent = "Load more";
    // Keeping one keyboard focus on the combobox follows the active-descendant pattern.
    next.tabIndex = -1;
    popup.className = "typed-choice-popup";
    popup.hidden = true;
    popup.append(list, status, next);
    clear.type = "button";
    clear.textContent = "×";
    clear.setAttribute("aria-label", "Clear choice");
    widget.append(input, clear, popup);
    widget._semanticTargetElement = input;

    let serial = 0;
    let composing = false;
    let active = -1;
    let open = false;
    let requested = "";
    let state = null;
    let renderedState = null;
    let options = [];

    function command(action, text = "") {
        sendEventToServer(widget, "choice search", { action, text, serial });
    }

    function setActive(index) {
        active = index;
        options.forEach((option, position) => {
            option.classList.toggle("active", position === active);
        });
        if (options[active]) {
            input.setAttribute("aria-activedescendant", options[active].id);
            options[active].scrollIntoView?.({ block: "nearest" });
        } else {
            input.removeAttribute("aria-activedescendant");
        }
    }

    function commit() {
        input.value = state?.keys.length ? state.selectedLabel : state?.freeText || "";
    }

    function close(cancel = true, restore = true) {
        if (!open) return;
        open = false;
        popup.hidden = true;
        input.setAttribute("aria-expanded", "false");
        setActive(-1);
        if (cancel) {
            serial++;
            command("cancel");
        }
        if (restore) commit();
    }

    function select(key, label) {
        if (input.disabled) return;
        sendEventToServer(widget, "choose keys", { keys: key ? [key] : [] });
        input.value = label;
        close(false, false);
    }

    function render(snapshot) {
        const formerKey = options[active]?.dataset.key;
        renderedState = snapshot;
        options = snapshot.options.map((item, position) => {
            const option = document.createElement("div");
            option.id = prefix + "-" + position;
            option.dataset.key = item.key;
            option.setAttribute("role", "option");
            option.setAttribute("aria-selected", String(snapshot.keys.includes(item.key)));
            option.textContent = item.label;
            if (item.disabled) option.setAttribute("aria-disabled", "true");
            return option;
        });
        list.replaceChildren(...options);
        setActive(
            formerKey === undefined
                ? -1
                : options.findIndex(option => option.dataset.key === formerKey)
        );
        next.hidden = !snapshot.more || snapshot.status === "loading";
        status.textContent =
            snapshot.status === "loading"
                ? "Loading choices…"
                : snapshot.status === "error"
                  ? "Choices unavailable. Edit the search to retry."
                  : options.length
                    ? ""
                    : "No choices";
        list.setAttribute("aria-busy", String(snapshot.status === "loading"));
    }

    function search(text) {
        if (open && requested === text) return;
        requested = text;
        open = true;
        input.setAttribute("aria-expanded", "true");
        popup.hidden = false;
        serial++;
        render({ options: [], keys: [], status: "loading", more: false });
        command("query", text);
    }

    widget._applyChoiceState = snapshot => {
        if (snapshot.serial < serial) return;
        serial = snapshot.serial;
        state = snapshot;
        input.placeholder = snapshot.placeholder;
        if (open) render(snapshot);
        else if (!composing) commit();
    };

    const disable = flag => {
        input.disabled = flag;
        clear.disabled = flag;
        if (flag) close();
    };
    Object.defineProperty(widget, "disabled", { get: () => input.disabled, set: disable });
    widget._closeSuggestions = close;
    widget._onDetached = () => close(false);
    listenWidget(widget, "compositionstart", () => (composing = true), undefined, input);
    listenWidget(
        widget,
        "compositionend",
        () => {
            composing = false;
            search(input.value);
        },
        undefined,
        input
    );
    listenWidget(
        widget,
        "input",
        event => {
            if (!composing && !event.isComposing) search(input.value);
        },
        undefined,
        input
    );
    listenWidget(
        widget,
        "click",
        () => {
            if (!open && !composing) search("");
        },
        undefined,
        input
    );
    listenWidget(widget, "focusout", event => {
        if (!widget.contains(event.relatedTarget)) close();
    });
    listenWidget(widget, "mousedown", event => {
        if (event.target !== input) event.preventDefault();
    });
    listenWidget(widget, "click", event => {
        const position = options.indexOf(event.target);
        const option = renderedState?.options[position];
        if (option && !option.disabled) select(option.key, option.label);
        if (event.target === next && state?.more) command("more");
        if (event.target === clear) {
            select("", "");
            input.focus();
        }
    });
    listenWidget(
        widget,
        "keydown",
        event => {
            if (event.isComposing || composing || event.key === "Process") return;
            if (event.key === "Escape" && open) {
                event.preventDefault();
                event.stopPropagation();
                close();
            } else if (event.key === "Tab" && open) {
                close();
            } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                event.preventDefault();
                if (!open) {
                    search("");
                    return;
                }
                const step = event.key === "ArrowDown" ? 1 : -1;
                let position = active === -1 && step < 0 ? options.length : active;
                position += step;
                while (options[position]?.getAttribute("aria-disabled") === "true") {
                    position += step;
                }
                if (options[position]) setActive(position);
                else if (step > 0 && state?.more && !next.hidden) command("more");
            } else if (event.key === "Enter" && open) {
                event.preventDefault();
                event.stopPropagation();
                const option = renderedState?.options[active];
                if (option && !option.disabled) select(option.key, option.label);
                else close();
            } else if (event.key === "Delete" && event.ctrlKey && !open) {
                event.preventDefault();
                select("", "");
            }
        },
        undefined,
        input
    );
    return widget;
}

function setChoiceState(data) {
    const widget = widgets[data.widget];
    if (!widget?._applyChoiceState || !data.state) return false;
    widget._applyChoiceState(data.state);
    return true;
}
