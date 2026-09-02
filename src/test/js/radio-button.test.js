import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const optionsSource = fs.readFileSync(
    new URL("../../main/html/scripts/options.js", import.meta.url),
    "utf8"
);
const librarySource = fs.readFileSync(
    new URL("../../main/html/scripts/lib.js", import.meta.url),
    "utf8"
);
const widgetsSource = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);

let dom;

afterEach(() => {
    dom?.window.close();
});

function createHarness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${optionsSource}\n${librarySource}\n
        configureUploadProtocol(4 * 1024, 128 * 1024 * 1024);
        window.__radioEvents = [];
        let clientId = "#1";
        function createEvent(widget, type, data) {
            window.__radioEvents.push({ widget, type, data });
        }
        function sendSynchronizeRequest() {}
        function sendEventToServer(widget, type, data) {
            if (widget._events[type]) {
                createEvent(widget, type, data);
                sendSynchronizeRequest();
            }
        }
        ${widgetsSource}
        window.__radioHarness = {
            createWidget,
            setCheckedFlag,
            setDisabledFlag,
            subscribeToEvent,
            widgets
        };
    `);
    return {
        ...dom.window.__radioHarness,
        events: dom.window.__radioEvents
    };
}

describe("radio button", () => {
    it("can be selected but not cleared by repeated clicks", () => {
        const harness = createHarness();
        const id = "#20";
        expect(harness.createWidget({ type: "radio button", widget: id })).toBe(true);
        expect(harness.subscribeToEvent({ widget: id, event: "check" })).toBe(true);
        const widget = harness.widgets[id];

        widget.click();

        expect(widget._selected).toBe(true);
        expect(harness.events).toEqual([
            { widget: id, type: "check", data: { state: true } }
        ]);

        widget.click();

        expect(widget._selected).toBe(true);
        expect(harness.events).toHaveLength(1);
    });

    it("can be cleared by the model and respects its disabled state", () => {
        const harness = createHarness();
        const id = "#21";
        harness.createWidget({ type: "radio button", widget: id });
        harness.subscribeToEvent({ widget: id, event: "check" });
        const widget = harness.widgets[id];

        harness.setCheckedFlag({ widget: id, checked: true });
        expect(widget._selected).toBe(true);
        harness.setCheckedFlag({ widget: id, checked: false });
        expect(widget._selected).toBe(false);

        harness.setDisabledFlag({ widget: id, disabled: true });
        widget.click();

        expect(widget._selected).toBe(false);
        expect(harness.events).toEqual([]);
    });
});
