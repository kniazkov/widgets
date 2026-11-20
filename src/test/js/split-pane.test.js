import { JSDOM } from "jsdom";
import { expect, it } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

it("clamps keyboard commits, accepts model changes and cleans up resize observation", () => {
    const dom = new JSDOM("<body></body>", { runScripts: "outside-only" });
    try {
        dom.window.eval(`
            class ResizeObserver {
                observe() { window.observing = true; }
                disconnect() { window.observing = false; }
            }
            const commits = [];
            function sendEventToServer(widget, type, data) { commits.push(data.position); }
            ${readScriptGroup("shared")}
            ${readScriptGroup("widgets")}
            window.splitApi = {createWidget, widgets, setSplitPane, commits};
        `);
        const { createWidget, widgets, setSplitPane, commits } = dom.window.splitApi;
        createWidget({ widget: "#10", type: "split pane" });
        const pane = widgets["#10"];
        dom.window.document.body.append(pane);
        pane.append(
            dom.window.document.createElement("section"),
            dom.window.document.createElement("section")
        );
        const snapshot = {
            label: "List",
            axis: "vertical",
            minimum: 20,
            maximum: 70,
            position: 66
        };
        const update = patch =>
            setSplitPane({ widget: "#10", snapshot: { ...snapshot, ...patch } });
        expect(update({})).toBe(true);
        pane._onAttached();
        const handle = pane.querySelector('[role="separator"]');
        function press(key, shiftKey = false) {
            handle.dispatchEvent(
                new dom.window.KeyboardEvent("keydown", { key, shiftKey, bubbles: true })
            );
        }
        press("ArrowDown", true);
        press("End");
        expect(commits).toEqual([70]);
        expect(update({ position: 23 })).toBe(true);
        expect(handle.getAttribute("aria-valuenow")).toBe("23");
        expect(update({ position: 0 })).toBe(false);
        press("ArrowUp", true);
        expect(commits).toEqual([70, 20]);
        expect(dom.window.observing).toBe(true);
        pane._onDetached();
        expect(dom.window.observing).toBe(false);
        pane._onAttached();
        pane._onDisposed();
        expect(dom.window.observing).toBe(false);
    } finally {
        dom.window.close();
    }
});
