import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const widgetsSource = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);
const librarySource = fs.readFileSync(
    new URL("../../main/html/scripts/lib.js", import.meta.url),
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
    dom.window.eval(`${librarySource}
        function sendEventToServer() {}
        ${widgetsSource}
        const sticky = widgetsLibrary["sticky panel"]();
        sticky._id = "#1";
        widgets[sticky._id] = sticky;
        document.body.appendChild(sticky);
        window.__stickyHarness = { sticky, setStickySide };
    `);
    return dom.window.__stickyHarness;
}

describe("sticky panel", () => {
    it("sticks to the top by default and may switch to the bottom", () => {
        const harness = createHarness();

        expect(harness.sticky.style.position).toBe("sticky");
        expect(harness.sticky.style.top).toBe("0px");
        expect(harness.sticky.style.bottom).toBe("");

        expect(harness.setStickySide({ widget: "#1", "sticky side": "bottom" })).toBe(true);
        expect(harness.sticky.style.top).toBe("");
        expect(harness.sticky.style.bottom).toBe("0px");
    });

    it("rejects an unsupported sticky side", () => {
        const harness = createHarness();

        expect(harness.setStickySide({ widget: "#1", "sticky side": "middle" })).toBe(false);
        expect(harness.sticky.style.top).toBe("0px");
        expect(harness.sticky.style.bottom).toBe("");
    });
});
