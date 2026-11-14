/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createDataTable() {
    const widget = document.createElement("section");
    const tools = document.createElement("div");
    const label = document.createElement("label");
    const labelText = document.createElement("span");
    const filter = document.createElement("input");
    const region = document.createElement("div");
    const table = document.createElement("table");
    const caption = document.createElement("caption");
    const head = document.createElement("tr");
    const header = document.createElement("thead");
    const body = document.createElement("tbody");
    const status = document.createElement("div");
    const pagination = document.createElement("div");
    const counter = document.createElement("span");
    const selection = document.createElement("span");
    const previous = document.createElement("button");
    const next = document.createElement("button");
    const retry = document.createElement("button");
    const sortCells = new Map();
    const rowNodes = new Map();
    let current;
    let disabled = false;
    let requestedFilter = "";
    let composing = false;
    let initialLoadRequested = false;
    let visible = false;
    const observer = new IntersectionObserver(entries => {
        visible = entries.at(-1)?.isIntersecting === true;
        loadWhenMounted();
    });

    widget.className = "widgets-data-table";
    tools.className = "widgets-data-table-tools";
    region.className = "widgets-data-table-scroll";
    region.setAttribute("role", "region");
    region.tabIndex = 0;
    filter.type = "search";
    filter.maxLength = 120;
    header.append(head);
    table.append(caption, header, body);
    region.append(table);
    status.setAttribute("role", "status");
    status.setAttribute("aria-atomic", "true");
    pagination.className = "widgets-data-table-pages";
    for (const button of [previous, next, retry]) button.type = "button";
    label.append(labelText, filter);
    tools.append(label);
    selection.setAttribute("aria-live", "polite");
    pagination.append(previous, counter, next, retry, selection);
    widget.append(tools, region, status, pagination);

    function command(action, value = "", checked = false) {
        if (!disabled && widget.isConnected && !widget._treePaused) {
            sendEventToServer(widget, "data table command", { action, value, checked });
        }
    }

    function loadWhenMounted() {
        if (
            visible &&
            widget.isConnected &&
            !widget._treePaused &&
            !disabled &&
            current?.state === "idle" &&
            !initialLoadRequested
        ) {
            initialLoadRequested = true;
            command("load");
        }
    }

    function sendFilter() {
        if (requestedFilter !== filter.value) {
            requestedFilter = filter.value;
            command("filter", requestedFilter);
        }
    }

    function initColumns(snapshot) {
        const first = document.createElement("th");
        first.scope = "col";
        first.textContent = snapshot.labels.choose;
        head.append(first);
        for (const { key, caption: title, sortable } of snapshot.columns) {
            const cell = document.createElement("th");
            cell.scope = "col";
            if (sortable) {
                const action = document.createElement("button");
                action.type = "button";
                action.textContent = title;
                action.dataset.column = key;
                cell.append(action);
                sortCells.set(key, cell);
            } else {
                cell.textContent = title;
            }
            head.append(cell);
        }
    }

    function makeRow(row) {
        const element = document.createElement("tr");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.dataset.row = row.key;
        element.insertCell().append(checkbox);
        for (let i = 0; i < row.cells.length; i++) element.insertCell();
        return { element, checkbox };
    }

    function refreshRows(snapshot) {
        const focused = body.ownerDocument.activeElement;
        const focusKey = body.contains(focused) && focused.dataset.row;
        const retained = new Set(snapshot.rows.map(row => row.key));
        for (const key of rowNodes.keys()) {
            if (!retained.has(key)) {
                rowNodes.get(key).element.remove();
                rowNodes.delete(key);
            }
        }
        snapshot.rows.forEach((row, position) => {
            let node = rowNodes.get(row.key);
            if (!node) {
                node = makeRow(row);
                rowNodes.set(row.key, node);
            }
            const { element, checkbox } = node;
            checkbox.checked = row.checked;
            checkbox.setAttribute(
                "aria-label",
                snapshot.labels.choose + ": " + (row.cells[0] || row.key)
            );
            checkbox.disabled = disabled;
            element.classList.toggle("is-selected", row.checked);
            row.cells.forEach((value, index) => {
                const cell = element.cells[index + 1];
                if (cell.textContent !== value) cell.textContent = value;
            });
            if (body.children[position] !== element) {
                body.insertBefore(element, body.children[position] || null);
            }
        });
        if (focusKey && body.ownerDocument.activeElement !== focused && widget.isConnected) {
            const checkbox = rowNodes.get(focusKey)?.checkbox;
            if (checkbox && !checkbox.disabled) checkbox.focus({ preventScroll: true });
            else if (!disabled) filter.focus({ preventScroll: true });
        }
    }

    function navigationButton(button, active) {
        button.setAttribute("aria-disabled", String(!active));
    }

    function refreshState() {
        if (!current) return;
        const { labels, state, index, pages, total, selected } = current;
        const busy = state === "loading";
        filter.disabled = disabled;
        for (const cell of sortCells.values()) cell.firstChild.disabled = disabled;
        for (const node of rowNodes.values()) node.checkbox.disabled = disabled;
        table.setAttribute("aria-busy", String(busy));
        navigationButton(previous, !disabled && !busy && index > 0);
        navigationButton(next, !disabled && !busy && index + 1 < pages);
        navigationButton(retry, !disabled && state === "error");
        retry.hidden = state !== "error";
        counter.textContent = labels.page + " " + (index + 1) + "/" + pages + " · " + total;
        const selectionText = labels.choose + ": " + selected;
        if (selection.textContent !== selectionText) selection.textContent = selectionText;
        status.textContent =
            busy || state === "idle"
                ? labels.loading
                : state === "error"
                  ? labels.error
                  : total === 0
                    ? labels.empty
                    : "";
    }

    widget._setDataTable = snapshot => {
        if (!current) {
            initColumns(snapshot);
            observer.observe(widget);
        }
        if (snapshot.state === "idle" && current?.state !== "idle") initialLoadRequested = false;
        current = snapshot;
        if (current.state !== "idle") observer.disconnect();
        else if (!widget._treePaused) observer.observe(widget);
        if (document.activeElement !== filter && !composing) {
            filter.value = snapshot.filter;
            requestedFilter = snapshot.filter;
        }
        caption.textContent = snapshot.caption;
        region.setAttribute("aria-label", snapshot.caption);
        labelText.textContent = snapshot.labels.filter;
        previous.textContent = snapshot.labels.previous;
        next.textContent = snapshot.labels.next;
        retry.textContent = snapshot.labels.retry;
        for (const [key, cell] of sortCells.entries()) {
            if (key === snapshot.sort) {
                cell.setAttribute("aria-sort", snapshot.descending ? "descending" : "ascending");
            } else {
                cell.removeAttribute("aria-sort");
            }
        }
        refreshRows(snapshot);
        refreshState();
        loadWhenMounted();
    };

    Object.defineProperty(widget, "disabled", {
        get: () => disabled,
        set: flag => {
            disabled = flag;
            refreshState();
            loadWhenMounted();
        }
    });
    widget._onAttached = () => {
        if (!current || current.state === "idle") observer.observe(widget);
    };
    widget._onDetached = () => {
        observer.disconnect();
        visible = false;
        initialLoadRequested = false;
    };
    widget._onDisposed = () => observer.disconnect();
    listenWidget(widget, "compositionstart", () => (composing = true), undefined, filter);
    listenWidget(
        widget,
        "compositionend",
        () => {
            composing = false;
            sendFilter();
        },
        undefined,
        filter
    );
    listenWidget(
        widget,
        "input",
        event => {
            if (!event.isComposing && !composing) sendFilter();
        },
        undefined,
        filter
    );
    listenWidget(widget, "change", event => {
        const target = event.target;
        if (target.dataset.row) {
            if (current?.state !== "loading") command("row", target.dataset.row, target.checked);
            else target.checked = !target.checked;
        }
    });
    listenWidget(widget, "click", event => {
        const node = event.target;
        if (node.dataset.column) command("sort", node.dataset.column);
        else if (node.getAttribute("aria-disabled") !== "true") {
            if (node === previous) command("previous");
            if (node === next) command("next");
            if (node === retry) command("load");
        }
    });
    return widget;
}

function setDataTable(data) {
    const widget = widgets[data.widget];
    if (!widget?._setDataTable || !data.snapshot) return false;
    widget._setDataTable(data.snapshot);
    return true;
}
