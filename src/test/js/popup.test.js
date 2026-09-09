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
        function sendEventToServer() {}
        ${widgetsSource}
        window.__popupHarness = {
            createWidget,
            appendChildWidget,
            removeChildWidget,
            setBackdropColor,
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
