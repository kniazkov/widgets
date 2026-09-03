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
        window.__selectionEvents = [];
        let clientId = "#1";
        function createEvent(widget, type, data) {
            window.__selectionEvents.push({ widget, type, data });
        }
        function sendSynchronizeRequest() {}
        function sendEventToServer(widget, type, data) {
            createEvent(widget, type, data);
            sendSynchronizeRequest();
        }
        ${widgetsSource}
        window.__dropDownHarness = {
            createWidget,
            setOptions,
            setOption,
            setSelectedIndex,
            setDisabledFlag,
            setPadding,
            widgets
        };
    `);
    return {
        ...dom.window.__dropDownHarness,
        events: dom.window.__selectionEvents
    };
}

describe("drop-down list", () => {
    it("renders fixed text options and applies model selection", () => {
        const harness = createHarness();
        const id = "#30";
        expect(harness.createWidget({ type: "drop down list", widget: id })).toBe(true);
        const widget = harness.widgets[id];
        expect(widget.style.appearance).toBe("none");
        expect(widget.style.backgroundPosition).toBe("95% 50%");
        expect(widget.style.backgroundRepeat).toBe("no-repeat");
        expect(widget.style.backgroundSize).toBe("12px 8px");
        expect(
            harness.setPadding({
                widget: id,
                padding: { left: "8px", right: "8px", top: "8px", bottom: "8px" }
            })
        ).toBe(true);
        expect(widget.style.paddingRight).toBe("calc(28px)");

        expect(harness.setOptions({ widget: id, options: ["Number", "Yes / no"] })).toBe(true);
        expect([...widget.options].map(option => option.text)).toEqual(["Number", "Yes / no"]);
        expect(widget.selectedIndex).toBe(-1);
        expect(harness.setSelectedIndex({ widget: id, "selected index": 1 })).toBe(true);
        expect(widget.selectedIndex).toBe(1);
        expect(harness.setSelectedIndex({ widget: id, "selected index": -1 })).toBe(true);
        expect(widget.selectedIndex).toBe(-1);
    });

    it("updates one option without changing positions", () => {
        const harness = createHarness();
        const id = "#31";
        harness.createWidget({ type: "drop down list", widget: id });
        harness.setOptions({ widget: id, options: ["First", "Second"] });

        expect(harness.setOption({ widget: id, index: 0, text: "Changed" })).toBe(true);
        expect([...harness.widgets[id].options].map(option => option.text)).toEqual([
            "Changed",
            "Second"
        ]);
        expect(harness.setOption({ widget: id, index: 2, text: "Invalid" })).toBe(false);
        expect(harness.widgets[id].options).toHaveLength(2);
    });

    it("sends stable indices and respects the native disabled state", () => {
        const harness = createHarness();
        const id = "#32";
        harness.createWidget({ type: "drop down list", widget: id });
        harness.setOptions({ widget: id, options: ["First", "Second"] });
        const widget = harness.widgets[id];
        widget.selectedIndex = 1;
        widget.dispatchEvent(new dom.window.Event("change"));

        expect(harness.events).toEqual([{ widget, type: "select", data: { index: 1 } }]);

        harness.setDisabledFlag({ widget: id, disabled: true });
        expect(widget.disabled).toBe(true);
    });
});
