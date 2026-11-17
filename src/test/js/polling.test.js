import { JSDOM } from "jsdom";
import { afterEach, expect, test } from "vitest";
import { readScript } from "./helpers/scripts.js";
let dom;
afterEach(() => dom?.window.close());
function harness() {
    dom = new JSDOM("<!doctype html>", { runScripts: "outside-only" });
    const win = dom.window;
    const tasks = new Map();
    let now = 0,
        serial = 0,
        visibility = "visible";
    win.Date.now = () => now;
    win.setTimeout = (fn, delay) => {
        tasks.set(++serial, { fn, at: now + delay });
        return serial;
    };
    win.clearTimeout = id => tasks.delete(id);
    Object.defineProperty(win.document, "visibilityState", { get: () => visibility });
    win.eval(readScript("shared/polling.js") + "\nwindow.scheduler = createPollingCoordinator();");
    return {
        scheduler: win.scheduler,
        tasks,
        advance(ms) {
            const end = now + ms;
            while (tasks.size) {
                const [id, task] = [...tasks].sort((a, b) => a[1].at - b[1].at)[0];
                if (task.at > end) break;
                now = task.at;
                tasks.delete(id);
                task.fn();
            }
            now = end;
        },
        visibility(value) {
            visibility = value;
            win.document.dispatchEvent(new win.Event("visibilitychange"));
        }
    };
}
test("one timer serves active and retained pages; bounded backoff and lease cap", () => {
    const h = harness();
    let activeCalls = 0,
        cachedCalls = 0;
    const active = h.scheduler.register(
        () => {
            activeCalls++;
            active.received(false);
        },
        () => true
    );
    const cached = h.scheduler.register(
        () => {
            cachedCalls++;
            cached.received(false);
        },
        () => false
    );
    expect(h.tasks.size).toBe(1);
    h.advance(30000);
    expect(activeCalls).toBe(4); // 2.5, 7.5, 17.5, 27.5 seconds
    expect(cachedCalls).toBe(1);
    active.received(true);
    h.advance(2500);
    expect(activeCalls).toBe(5);
    active.dispose();
    cached.dispose();
    expect(h.tasks.size).toBe(0);
    let capped = 0;
    const lease = h.scheduler.register(
        () => {
            capped++;
            lease.received(false);
        },
        () => false,
        1000
    );
    h.advance(3000);
    expect(capped).toBe(3);
});
test("hidden document slows down; becoming visible wakes only the active page immediately", () => {
    const h = harness();
    let calls = 0;
    const active = h.scheduler.register(
        () => {
            calls++;
            active.received(false);
        },
        () => true
    );
    h.visibility("hidden");
    h.advance(2500);
    expect(calls).toBe(1);
    h.advance(29000);
    expect(calls).toBe(1);
    h.visibility("visible");
    h.advance(0);
    expect(calls).toBe(2);
    active.dispose();
    h.visibility("visible");
    expect(h.tasks.size).toBe(0);
});
