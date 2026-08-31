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
        function createEvent() {}
        function sendSynchronizeRequest() {}
        function sendEventToServer() {}
        ${widgetsSource}
        window.__styleHarness = {
            createWidget,
            refreshWidget,
            setBoxShadow,
            setOutline,
            setCursor,
            setTransition,
            setBoxSizing,
            widgets
        };
    `);
    return dom.window.__styleHarness;
}

describe("modern style properties", () => {
    it("applies state-dependent visual properties", () => {
        const harness = createHarness();
        const id = "#11";
        harness.createWidget({ type: "button", widget: id });
        const widget = harness.widgets[id];

        expect(
            harness.setBoxShadow({
                widget: id,
                state: "focused",
                "box shadow": "0px 0px 0px 3px rgba(0,0,255,0.25)"
            })
        ).toBe(true);
        expect(
            harness.setOutline({
                widget: id,
                state: "focused",
                outline: {
                    color: { r: 0, g: 0, b: 255 },
                    style: "solid",
                    width: "2px",
                    offset: "2px"
                }
            })
        ).toBe(true);
        expect(harness.setCursor({ widget: id, state: "focused", cursor: "pointer" })).toBe(true);

        widget._states.focused = true;
        harness.refreshWidget(widget);

        expect(widget.style.boxShadow).toBe("0px 0px 0px 3px rgba(0,0,255,0.25)");
        expect(widget.style.outlineColor).toBe("rgb(0, 0, 255)");
        expect(widget.style.outlineStyle).toBe("solid");
        expect(widget.style.outlineWidth).toBe("2px");
        expect(widget.style.outlineOffset).toBe("2px");
        expect(widget.style.cursor).toBe("pointer");
    });

    it("applies state-independent transition and box sizing", () => {
        const harness = createHarness();
        const id = "#12";
        harness.createWidget({ type: "input field", widget: id });
        const widget = harness.widgets[id];

        expect(harness.setTransition({ widget: id, transition: "all 150ms ease-out 0ms" })).toBe(
            true
        );
        expect(harness.setBoxSizing({ widget: id, "box sizing": "border-box" })).toBe(true);

        expect(widget.style.transition).toBe("all 150ms ease-out 0ms");
        expect(widget.style.boxSizing).toBe("border-box");
    });

    it("rejects malformed values", () => {
        const harness = createHarness();
        const id = "#13";
        harness.createWidget({ type: "button", widget: id });

        expect(harness.setBoxShadow({ widget: id, state: "normal", "box shadow": 42 })).toBe(false);
        expect(harness.setOutline({ widget: id, state: "normal", outline: {} })).toBe(false);
        expect(harness.setOutline({ widget: id, state: "normal", outline: null })).toBe(false);
        expect(harness.setCursor({ widget: id, state: "normal", cursor: null })).toBe(false);
        expect(harness.setTransition({ widget: id, transition: null })).toBe(false);
        expect(harness.setBoxSizing({ widget: id, "box sizing": null })).toBe(false);
    });
});
