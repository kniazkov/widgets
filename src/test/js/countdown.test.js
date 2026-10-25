import { JSDOM } from "jsdom";
import { afterEach, expect, it, vi } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

let dom;
afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        pretendToBeVisual: true
    });
    const window = dom.window;
    let monotonic = 0;
    let hidden = false;
    const tasks = new Map();
    let serial = 0;
    window.setTimeout = callback => {
        tasks.set(++serial, callback);
        return serial;
    };
    window.clearTimeout = id => tasks.delete(id);
    window.performance.now = () => monotonic;
    Object.defineProperty(window.document, "hidden", { get: () => hidden });
    window.requests = vi.fn();
    window.eval(`const log = () => {}; const sendSynchronizeRequest = window.requests;
        ${readScriptGroup("widgets")}
        window.api = { createWidget, setCountdownDeadline, sampleCountdownClock,
            refreshCountdowns, disposeCountdowns, removeChildWidget, appendChildWidget,
            widgets };`);
    const api = window.api;
    const holder = window.document.createElement("div");
    window.document.body.append(holder);
    api.widgets.holder = holder;
    function add(id = "timer", deadline = 10000) {
        api.createWidget({ type: "countdown", widget: id });
        api.setCountdownDeadline({ widget: id, deadline });
        api.appendChildWidget({ widget: id, container: "holder" });
        return api.widgets[id];
    }
    return {
        api,
        window,
        tasks,
        holder,
        add,
        advance(ms) {
            monotonic += ms;
            const pending = [...tasks.values()];
            tasks.clear();
            pending.forEach(callback => callback());
        },
        background(value) {
            hidden = value;
            window.document.dispatchEvent(new window.Event("visibilitychange"));
        }
    };
}

it("computes elapsed monotonic time after delayed callbacks, ignores wall clock, stops at zero", () => {
    const h = harness();
    h.api.sampleCountdownClock(0);
    const timer = h.add();
    expect(timer.textContent).toBe("00:10");
    h.window.Date.now = () => 9999999999999;
    h.advance(7300);
    expect(timer.textContent).toBe("00:03");
    h.advance(3000);
    expect(timer.textContent).toBe("00:00");
    expect(h.tasks.size).toBe(0);
    expect(h.window.requests).toHaveBeenCalledTimes(0);
});

it("replaces deadlines atomically and accepts fresh server samples without a new deadline", () => {
    const h = harness();
    h.api.sampleCountdownClock(1000);
    const timer = h.add("timer", 3602000);
    expect(timer.textContent).toBe("1:00:01");
    h.api.sampleCountdownClock(3601500);
    expect(timer.textContent).toBe("00:01");
    h.api.setCountdownDeadline({ widget: "timer", deadline: -1 });
    expect(timer.textContent).toBe("00:00");
    h.api.setCountdownDeadline({ widget: "timer", deadline: 3603500 });
    expect(timer.textContent).toBe("00:02");
    for (const deadline of [NaN, Infinity, "300", 8640000000000001]) {
        expect(h.api.setCountdownDeadline({ widget: "timer", deadline })).toBe(false);
    }
    expect(timer.textContent).toBe("00:02");
});

it("suspends in background and resynchronizes once after sleep even if monotonic time stopped", () => {
    const h = harness();
    h.api.sampleCountdownClock(0);
    const timer = h.add();
    h.background(true);
    expect(h.tasks.size).toBe(0);
    h.background(false);
    expect(h.window.requests).toHaveBeenCalledTimes(1);
    expect(timer.textContent).toBe("--:--");
    h.api.sampleCountdownClock(20000);
    expect(timer.textContent).toBe("00:00");
    expect(h.tasks.size).toBe(0);
});

it("cleans up nested removals, repeated attachment, navigation cache and runtime disposal", () => {
    const h = harness();
    const addListener = vi.spyOn(h.window.document, "addEventListener");
    const removeListener = vi.spyOn(h.window.document, "removeEventListener");
    h.api.sampleCountdownClock(0);
    const timer = h.add();
    h.add("other");
    expect(h.tasks.size).toBe(1);
    for (let index = 0; index < 5; index++) {
        h.holder.remove();
        h.api.refreshCountdowns();
        expect(h.tasks.size).toBe(0);
        h.advance(1000);
        h.window.document.body.append(h.holder);
        h.api.refreshCountdowns();
        expect(h.tasks.size).toBe(1);
    }
    expect(timer.textContent).toBe("00:05");
    h.api.widgets.body = h.window.document.body;
    h.api.removeChildWidget({ widget: "holder", container: "body" });
    expect(h.tasks.size).toBe(0);
    h.api.appendChildWidget({ widget: "holder", container: "body" });
    expect(h.tasks.size).toBe(1);
    h.api.removeChildWidget({ widget: "timer", container: "holder" });
    h.api.removeChildWidget({ widget: "other", container: "holder" });
    expect(h.tasks.size).toBe(0);
    h.api.appendChildWidget({ widget: "timer", container: "holder" });
    h.api.disposeCountdowns();
    h.api.refreshCountdowns();
    expect(h.tasks.size).toBe(0);
    const count = spy => spy.mock.calls.filter(call => call[0] === "visibilitychange").length;
    expect(count(addListener)).toBe(count(removeListener));
    h.background(false);
    expect(h.window.requests).not.toHaveBeenCalled();
});
