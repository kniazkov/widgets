/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

function createFeedback() {
    const widget = document.createElement("section");
    const frame = document.createElement("div");
    const message = document.createElement("div");
    const meter = document.createElement("progress");
    const host = document.createElement("div");
    const notifications = new Map();
    let timer;
    let disposed = false;
    frame.className = "widgets-feedback";
    message.setAttribute("aria-atomic", "true");
    meter.max = 100;
    meter.hidden = true;
    frame.append(message, meter, host);
    widget.append(frame);
    widget._childHost = host;
    const stop = () => {
        clearTimeout(timer);
        timer = undefined;
    };
    const tick = () => {
        stop();
        if (disposed || widget._treePaused || !widget.isConnected) return;
        let next = Infinity;
        for (const item of notifications.values()) {
            if (!item.deadline || item.dismissed) continue;
            const remaining = item.deadline - Date.now();
            if (remaining <= 0) {
                // Keep a focused close control until focus leaves it.
                if (item.node.contains(document.activeElement)) next = Math.min(next, 250);
                else {
                    item.dismissed = true;
                    item.scope.abort();
                    item.node.remove();
                }
            } else next = Math.min(next, remaining);
        }
        if (Number.isFinite(next)) timer = setTimeout(tick, next);
    };
    widget._onDetached = stop;
    widget._onAttached = tick;
    widget._onDisposed = () => {
        disposed = true;
        stop();
        for (const item of notifications.values()) item.scope.abort();
        notifications.clear();
    };
    widget._feedback = data => {
        if (disposed) return false;
        if (data.kind === "toast") {
            message.hidden = true;
            const items = data.items || [];
            const ids = new Set(items.map(item => item.id));
            for (const [id, item] of notifications) {
                if (!ids.has(id)) {
                    item.scope.abort();
                    item.node.remove();
                    notifications.delete(id);
                }
            }
            for (const item of items) {
                if (notifications.has(item.id)) continue;
                const node = document.createElement("div");
                const text = document.createElement("div");
                const close = document.createElement("button");
                node.className = "widgets-feedback widgets-feedback-" + item.severity;
                text.setAttribute("role", item.severity === "error" ? "alert" : "status");
                text.setAttribute("aria-atomic", "true");
                text.textContent = item.text;
                close.type = "button";
                close.textContent = data.dismiss;
                const scope = new AbortController();
                const entry = {
                    scope,
                    node,
                    dismissed: false,
                    deadline: item.ttl ? Date.now() + item.ttl : 0
                };
                close.addEventListener(
                    "click",
                    () => {
                        if (disposed || entry.dismissed || !widget.isConnected) return;
                        entry.dismissed = true;
                        node.remove();
                        scope.abort();
                        sendEventToServer(widget, "toast dismiss", { id: item.id });
                        tick();
                    },
                    { signal: scope.signal }
                );
                node.append(text, close);
                host.append(node);
                notifications.set(item.id, entry);
            }
            tick();
            return true;
        }
        frame.className = "widgets-feedback widgets-feedback-" + data.severity;
        const role = data.severity === "error" ? "alert" : "status";
        if (message.getAttribute("role") !== role) message.setAttribute("role", role);
        // Updating a percentage must not rewrite/reannounce an unchanged status message.
        if (message.textContent !== data.text) message.textContent = data.text;
        message.hidden = !data.text;
        meter.hidden = data.kind !== "progress" || !data.running;
        if (!meter.hidden) {
            meter.setAttribute("aria-label", data.text);
            if (data.progress < 0) meter.removeAttribute("value");
            else meter.value = data.progress;
        }
        meter.setAttribute("aria-busy", String(data.running === true));
        return true;
    };
    return widget;
}

function setFeedback(data) {
    return widgets[data.widget]?._feedback?.(data.snapshot) === true;
}
