/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createTreeView() {
    const tree = document.createElement("div");
    tree.className = "widgets-tree-view";
    tree.setAttribute("role", "tree");
    tree.tabIndex = 0;
    let rows = [];
    let items = new Map();
    let active = "";
    let disabled = false;
    const requested = new Map();

    function focusKey(key) {
        items.get(active)?.classList.remove("active");
        active = key;
        const item = items.get(key);
        if (item) {
            item.classList.add("active");
            tree.setAttribute("aria-activedescendant", item.id);
        } else tree.removeAttribute("aria-activedescendant");
    }

    function command(action, key) {
        if (disabled || tree._treePaused || !tree.isConnected) return;
        if (action !== "select") requested.set(key, action === "open");
        sendEventToServer(tree, "tree command", { action, key });
    }

    tree._renderTree = snapshot => {
        const previous = rows;
        const previousIndex = previous.findIndex(row => row.key === active);
        const parents = new Map(previous.map(row => [row.key, row.parent]));
        rows = snapshot.rows;
        const groups = new Map();
        const fragment = document.createDocumentFragment();
        groups.set("", fragment);
        items = new Map();
        for (const [index, row] of rows.entries()) {
            const item = document.createElement("div");
            item.id = "widgets-tree-" + tree._id + "-" + index;
            item.dataset.key = row.key;
            item.setAttribute("role", "treeitem");
            item.setAttribute("aria-selected", String(row.key === snapshot.selected));
            item.setAttribute("aria-label", row.label);
            const line = document.createElement("div");
            line.className = "widgets-tree-line";
            const toggle = document.createElement("span");
            toggle.className = "widgets-tree-toggle";
            toggle.setAttribute("aria-hidden", "true");
            if (row.branch) {
                item.setAttribute("aria-expanded", String(row.open));
                toggle.textContent = row.open ? "▾" : "▸";
                toggle.dataset.toggle = "";
            }
            const label = document.createElement("span");
            label.textContent = row.label;
            line.append(toggle, label);
            item.append(line);
            groups.get(row.parent).append(item);
            if (row.open) {
                const group = document.createElement("div");
                group.setAttribute("role", "group");
                item.append(group);
                groups.set(row.key, group);
                if (row.status) {
                    const status = document.createElement("div");
                    status.setAttribute("role", "status");
                    status.textContent = row.status;
                    group.append(status);
                }
            }
            items.set(row.key, item);
            if (requested.get(row.key) === row.open) requested.delete(row.key);
        }
        for (const key of requested.keys()) if (!items.has(key)) requested.delete(key);
        if (!rows.length) {
            const status = document.createElement("div");
            status.setAttribute("role", "status");
            status.textContent = snapshot.empty;
            fragment.append(status);
        }
        tree.replaceChildren(fragment);
        let key = active;
        while (key && !items.has(key)) key = parents.get(key);
        focusKey(key || rows[Math.max(0, Math.min(previousIndex, rows.length - 1))]?.key || "");
    };

    function open(row) {
        return requested.get(row.key) ?? row.open;
    }

    Object.defineProperty(tree, "disabled", {
        get: () => disabled,
        set: value => {
            disabled = value;
            tree.tabIndex = value ? -1 : 0;
            tree.setAttribute("aria-disabled", String(value));
        }
    });

    listenWidget(tree, "click", event => {
        const item = event.target.closest('[role="treeitem"]');
        if (!item || disabled) return;
        const row = rows.find(value => value.key === item.dataset.key);
        focusKey(row.key);
        tree.focus({ preventScroll: true });
        if (event.target.closest("[data-toggle]")) command(open(row) ? "close" : "open", row.key);
        else command("select", row.key);
    });

    listenWidget(tree, "keydown", event => {
        if (disabled || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
        const index = rows.findIndex(row => row.key === active);
        const row = rows[index];
        if (!row) return;
        let next = active;
        switch (event.key) {
            case "ArrowDown":
                next = rows[index + 1]?.key ?? active;
                break;
            case "ArrowUp":
                next = rows[index - 1]?.key ?? active;
                break;
            case "Home":
                next = rows[0].key;
                break;
            case "End":
                next = rows.at(-1).key;
                break;
            case "ArrowRight":
                if (row.branch && !open(row)) command("open", active);
                else if (rows[index + 1]?.parent === active) next = rows[index + 1].key;
                break;
            case "ArrowLeft":
                if (row.branch && open(row)) command("close", active);
                else next = row.parent || active;
                break;
            case "Enter":
            case " ":
                command("select", active);
                break;
            default:
                return;
        }
        event.preventDefault();
        focusKey(next);
        items.get(next).firstElementChild.scrollIntoView({ block: "nearest" });
    });
    return tree;
}

function setTreeView(data) {
    const widget = widgets[data.widget];
    if (!widget?._renderTree || !Array.isArray(data.snapshot?.rows)) return false;
    widget._renderTree(data.snapshot);
    return true;
}
