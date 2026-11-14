import { readScript, readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

let dom;

afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost"
    });
    dom.window.eval(`
        ${readScript("options.js")}
        ${readScriptGroup("shared")}
        ${readScriptGroup("widgets")}
        const observers = [];
        window.ResizeObserver = class {
            constructor() { this.observeCalls = 0; this.disconnectCalls = 0; observers.push(this); }
            observe() { this.observeCalls++; }
            disconnect() { this.disconnectCalls++; }
        };
        let reloads = 0;
        const pageContext = null;
        function reloadCurrentPage() { reloads++; }
        function sendEventToServer() {}
        function sendSynchronizeRequest() {}
        createWidget({widget: "root", type: "root"});
        window.harness = {
            widgets, createWidget, appendChildWidget, setChildWidget, removeChildWidget,
            disposeWidget, trimDetachedWidgets, disposeAllWidgets, detachedWidgets,
            observers, pendingUploads, activeUploads,
            get reloads() { return reloads; }
        };
    `);
    return dom.window.harness;
}

describe("widget subtree lifecycle", () => {
    it("releases thousands of final removals including descendants and listeners", () => {
        const h = harness();
        let disposed = 0;
        for (let i = 0; i < 1500; i++) {
            const widget = "parent" + i;
            const child = "child" + i;
            h.createWidget({ widget, type: "section" });
            h.createWidget({ widget: child, type: "button" });
            h.appendChildWidget({ widget: child, container: widget });
            h.appendChildWidget({ widget, container: "root" });
            const node = h.widgets[child];
            node._onDisposed = () => disposed++;
            h.removeChildWidget({ widget, container: "root" });
            h.disposeWidget({ widget });
            expect(node._listenerScope.signal.aborted).toBe(true);
            expect(h.disposeWidget({ widget })).toBe(true);
        }
        expect(Object.keys(h.widgets)).toEqual(["root"]);
        expect(h.detachedWidgets.size).toBe(0);
        expect(disposed).toBe(1500);
    });

    it("retains DOM state through detach and replacement and retires it with the page", () => {
        const h = harness();
        h.createWidget({ widget: "old", type: "input field" });
        h.createWidget({ widget: "new", type: "input field" });
        h.createWidget({ widget: "section", type: "section" });
        h.appendChildWidget({ widget: "section", container: "root" });
        h.setChildWidget({ widget: "old", container: "section" });
        const input = h.widgets.old;
        input.value = "unsent local state";
        h.setChildWidget({ widget: "new", container: "section" });
        h.trimDetachedWidgets();
        expect(input.isConnected).toBe(false);
        expect(input._listenerScope.signal.aborted).toBe(false);
        h.setChildWidget({ widget: "old", container: "section" });
        expect(h.widgets.old).toBe(input);
        expect(input.value).toBe("unsent local state");
        h.disposeAllWidgets();
        expect(Object.keys(h.widgets)).toEqual([]);
        expect(h.detachedWidgets.size).toBe(0);
        expect(input._listenerScope.signal.aborted).toBe(true);
        expect(dom.window.document.body.childElementCount).toBe(0);
    });

    it("pauses nested observers and releases retired picker and upload resources", () => {
        const h = harness();
        h.createWidget({ widget: "section", type: "section" });
        h.createWidget({ widget: "zoom", type: "zoom decorator" });
        h.createWidget({ widget: "loader", type: "file loader" });
        h.appendChildWidget({ widget: "zoom", container: "section" });
        h.appendChildWidget({ widget: "loader", container: "section" });
        h.appendChildWidget({ widget: "section", container: "root" });
        const observer = h.observers[0];
        const loader = h.widgets.loader;
        const queued = { widget: loader, source: { name: "fixture" }, ready: false };
        h.pendingUploads.push(queued);
        loader._onClick();
        expect(dom.window.document.querySelectorAll("input[type=file]")).toHaveLength(1);
        h.removeChildWidget({ widget: "section", container: "root" });
        expect(observer.disconnectCalls).toBe(1);
        expect(dom.window.document.querySelectorAll("input[type=file]")).toHaveLength(0);
        expect(queued.widget).toBe(loader);
        h.appendChildWidget({ widget: "section", container: "root" });
        expect(observer.observeCalls).toBeGreaterThan(2);
        const active = { widget: loader, source: { name: "active fixture" }, ready: true };
        h.activeUploads.push(active);
        h.disposeWidget({ widget: "section" });
        expect(observer.disconnectCalls).toBe(2);
        expect(h.pendingUploads).toHaveLength(0);
        expect(h.activeUploads).toHaveLength(0);
        for (const file of [queued, active]) {
            expect(file.widget).toBeNull();
            expect(file.source).toBeNull();
        }
    });

    it("bounds legacy detachments by registered nodes and requests recovery on an evicted attach", () => {
        const h = harness();
        for (let i = 0; i < 200; i++) {
            const widget = "parent" + i;
            const child = "child" + i;
            h.createWidget({ widget, type: "section" });
            h.createWidget({ widget: child, type: "text" });
            h.appendChildWidget({ widget: child, container: widget });
            h.appendChildWidget({ widget, container: "root" });
            h.removeChildWidget({ widget, container: "root" });
            h.trimDetachedWidgets();
        }
        expect(Object.keys(h.widgets)).toHaveLength(129);
        expect(h.widgets.parent0).toBeUndefined();
        expect(h.widgets.child0).toBeUndefined();
        expect(h.appendChildWidget({ widget: "parent199", container: "root" })).toBe(true);
        expect(h.appendChildWidget({ widget: "parent0", container: "root" })).toBe(false);
        expect(h.reloads).toBe(1);
    });
});
