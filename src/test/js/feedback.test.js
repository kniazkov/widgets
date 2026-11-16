import { JSDOM } from "jsdom";
import { afterEach, expect, it, vi } from "vitest";
import { readScript } from "./helpers/scripts.js";

let dom;
afterEach(() => {
    dom?.window.close();
    vi.useRealTimers();
});
function harness() {
    dom = new JSDOM("<!doctype html><body><input></body>", { runScripts: "outside-only" });
    dom.window.eval(`const widgets = {}; const events = [];
        function sendEventToServer(widget, type, data) { events.push({type, data}); }
        ${readScript("widgets/feedback.js")}
        window.api = { createFeedback, events };`);
    const widget = dom.window.api.createFeedback();
    dom.window.document.body.append(widget);
    return { widget, events: dom.window.api.events };
}
it("renders plain status text without stealing focus or rewriting it on progress ticks", () => {
    const { widget } = harness();
    const input = dom.window.document.querySelector("input");
    input.focus();
    const data = {
        kind: "progress",
        text: "<b>Загрузка</b>",
        severity: "info",
        progress: -1,
        running: true
    };
    widget._feedback(data);
    const text = widget.firstChild.firstChild.firstChild;
    expect(widget.querySelector("b")).toBeNull();
    expect(widget.querySelector("progress").hasAttribute("value")).toBe(false);
    widget._feedback({ ...data, progress: 40 });
    expect(widget.firstChild.firstChild.firstChild).toBe(text);
    expect(widget.querySelector("progress").value).toBe(40);
    expect(dom.window.document.activeElement).toBe(input);
    widget._feedback({ ...data, text: "Ошибка", severity: "error", running: false });
    expect(widget.firstChild.firstChild.getAttribute("role")).toBe("alert");
    expect(widget.querySelector("progress").hidden).toBe(true);
});
it("retains unchanged toast nodes and dismisses only the selected ID", () => {
    const { widget, events } = harness();
    const data = {
        kind: "toast",
        dismiss: "Закрыть",
        items: [{ id: "1", text: "Ошибка", severity: "error", ttl: 0 }]
    };
    widget._feedback(data);
    const node = widget.querySelector("[role=alert]");
    widget._feedback(data);
    expect(widget.querySelector("[role=alert]")).toBe(node);
    widget.querySelector("button").click();
    expect(events[0].data.id).toBe("1");
    widget._feedback(data);
    expect(widget.querySelector("[role=alert]")).toBeNull();
});
it("cleans timers on detach and disposal and expires notifications on resume", () => {
    vi.useFakeTimers();
    const { widget } = harness();
    dom.window.setTimeout = setTimeout;
    dom.window.clearTimeout = clearTimeout;
    dom.window.Date = Date;
    widget._feedback({
        kind: "toast",
        dismiss: "Close",
        items: [{ id: "1", text: "Saved", severity: "success", ttl: 1000 }]
    });
    expect(vi.getTimerCount()).toBe(1);
    widget._treePaused = true;
    widget._onDetached();
    expect(vi.getTimerCount()).toBe(0);
    vi.advanceTimersByTime(1500);
    widget._treePaused = false;
    widget._onAttached();
    expect(widget.querySelector("button")).toBeNull();
    widget._onDisposed();
    expect(widget._feedback({ kind: "toast", items: [] })).toBe(false);
    expect(vi.getTimerCount()).toBe(0);
});

it("does not remove a focused notification on its timer and releases listeners on disposal", () => {
    vi.useFakeTimers();
    const { widget, events } = harness();
    dom.window.setTimeout = setTimeout;
    dom.window.clearTimeout = clearTimeout;
    dom.window.Date = Date;
    widget._feedback({
        kind: "toast",
        dismiss: "Close",
        items: [
            { id: "1", text: "Saved", severity: "success", ttl: 1000 },
            { id: "2", text: "Error", severity: "error", ttl: 0 }
        ]
    });
    const close = widget.querySelector("button");
    close.focus();
    vi.advanceTimersByTime(1500);
    expect(close.isConnected).toBe(true);
    expect(dom.window.document.activeElement).toBe(close);
    dom.window.document.querySelector("input").focus();
    vi.advanceTimersByTime(250);
    expect(close.isConnected).toBe(false);
    const old = widget.querySelector("button");
    widget._onDisposed();
    old.click();
    expect(events).toHaveLength(0);
    expect(vi.getTimerCount()).toBe(0);
});
