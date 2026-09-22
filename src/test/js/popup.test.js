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
        const events = [];
        function sendEventToServer(widget, type, data) { events.push({widget, type, data}); }
        ${widgetsSource}
        window.__popupHarness = {
            createWidget,
            appendChildWidget,
            removeChildWidget,
            setBackdropColor,
            setCloseOnOutsideClick,
            events,
            setHorzAlignment,
            setVertAlignment,
            widgets
        };
    `);
    return dom.window.__popupHarness;
}

describe("popup widgets", () => {
    it("positions a non-modal popup against the viewport", () => {
        const harness = createHarness();
        harness.createWidget({ type: "popup", widget: "#10" });
        const popup = harness.widgets["#10"];

        expect(popup.style.position).toBe("fixed");
        expect(popup.style.left).toBe("50%");
        expect(popup.style.top).toBe("50%");
        expect(popup.style.transform).toBe("translate(-50%, -50%)");

        harness.setHorzAlignment({ widget: "#10", "horz alignment": "right" });
        harness.setVertAlignment({ widget: "#10", "vert alignment": "bottom" });

        expect(popup.style.left).toBe("");
        expect(popup.style.right).toBe("0px");
        expect(popup.style.top).toBe("");
        expect(popup.style.bottom).toBe("0px");
        expect(popup.style.transform).toBe("translate(0, 0)");
    });

    it("attaches and removes a modal backdrop together with the popup", () => {
        const harness = createHarness();
        harness.createWidget({ type: "root", widget: "#1" });
        harness.createWidget({ type: "modal popup", widget: "#11" });
        const popup = harness.widgets["#11"];

        expect(
            harness.setBackdropColor({
                widget: "#11",
                "backdrop color": { r: 255, g: 255, b: 255, a: 0.75 }
            })
        ).toBe(true);
        harness.appendChildWidget({ widget: "#11", container: "#1" });

        expect(dom.window.document.body.children).toHaveLength(2);
        expect(dom.window.document.body.children[0]).toBe(popup._backdrop);
        expect(dom.window.document.body.children[1]).toBe(popup);
        expect(popup._backdrop.style.backgroundColor).toBe("rgba(255, 255, 255, 0.75)");

        harness.removeChildWidget({ widget: "#11", container: "#1" });
        expect(dom.window.document.body.children).toHaveLength(0);
    });
});

function backdropPress(target, button = 0) {
    target.dispatchEvent(new dom.window.MouseEvent("pointerdown", { bubbles: true, button }));
}
function backdropClick(target, button = 0) {
    target.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true, button, detail: 1 }));
}

describe("outside dismissal", () => {
    it("reacts to model changes and waits for server removal", () => {
        const h = createHarness();
        h.createWidget({ type: "root", widget: "#1" });
        h.createWidget({ type: "modal popup", widget: "#2" });
        h.appendChildWidget({ widget: "#2", container: "#1" });
        const popup = h.widgets["#2"],
            backdrop = popup._backdrop;
        backdropPress(backdrop);
        backdropClick(backdrop);
        expect(h.events).toHaveLength(0);
        expect(h.setCloseOnOutsideClick({ widget: "#2", "close on outside click": true })).toBe(
            true
        );
        backdropPress(popup);
        backdropClick(popup);
        expect(h.events).toHaveLength(0);
        backdropPress(backdrop);
        backdropClick(backdrop);
        expect(h.events).toHaveLength(1);
        expect(h.events[0].type).toBe("dismiss");
        expect(h.events[0].widget).toBe(popup);
        expect(popup.isConnected).toBe(true);
        h.setCloseOnOutsideClick({ widget: "#2", "close on outside click": false });
        backdropPress(backdrop);
        backdropClick(backdrop);
        expect(h.events).toHaveLength(1);
        h.removeChildWidget({ widget: "#2", container: "#1" });
        expect(backdrop.isConnected).toBe(false);
        expect(popup.isConnected).toBe(false);
    });
    it("ignores right clicks, cancelled gestures and drags starting inside", () => {
        const h = createHarness();
        h.createWidget({ type: "modal popup", widget: "#2" });
        h.setCloseOnOutsideClick({ widget: "#2", "close on outside click": true });
        const popup = h.widgets["#2"],
            backdrop = popup._backdrop;
        backdropPress(backdrop, 2);
        backdropClick(backdrop, 2);
        backdropPress(backdrop);
        backdrop.dispatchEvent(new dom.window.Event("pointercancel"));
        backdropClick(backdrop);
        backdropPress(popup);
        backdropClick(backdrop);
        expect(h.events).toHaveLength(0);
        expect(
            h.setCloseOnOutsideClick({ widget: "#missing", "close on outside click": true })
        ).toBe(false);
    });
});
