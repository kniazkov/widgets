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
        let clientId = "#1";
        function sendEventToServer() {}
        ${widgetsSource}
        window.__tableHarness = {
            createWidget,
            appendChildWidget,
            insertChildWidget,
            removeChildWidget,
            widgets
        };
    `);
    return dom.window.__tableHarness;
}

function childIds(widget) {
    return [...widget.children].map(child => child._id);
}

describe("table row mutations", () => {
    it("inserts rows at the beginning, middle and end", () => {
        const harness = createHarness();
        harness.createWidget({ type: "table", widget: "#10" });
        harness.createWidget({ type: "row", widget: "#11" });
        harness.createWidget({ type: "row", widget: "#12" });
        harness.createWidget({ type: "row", widget: "#13" });
        harness.createWidget({ type: "row", widget: "#14" });
        harness.appendChildWidget({ widget: "#12", container: "#10" });

        expect(harness.insertChildWidget({ widget: "#11", container: "#10", index: 0 })).toBe(true);
        expect(harness.insertChildWidget({ widget: "#13", container: "#10", index: 2 })).toBe(true);
        expect(harness.insertChildWidget({ widget: "#14", container: "#10", index: 1 })).toBe(true);
        expect(childIds(harness.widgets["#10"])).toEqual(["#11", "#14", "#12", "#13"]);
    });

    it("removes an inserted row and rejects invalid positions", () => {
        const harness = createHarness();
        harness.createWidget({ type: "table", widget: "#20" });
        harness.createWidget({ type: "row", widget: "#21" });
        harness.createWidget({ type: "row", widget: "#22" });
        harness.appendChildWidget({ widget: "#21", container: "#20" });
        harness.insertChildWidget({ widget: "#22", container: "#20", index: 1 });

        expect(harness.insertChildWidget({ widget: "#22", container: "#20", index: -1 })).toBe(
            false
        );
        expect(harness.insertChildWidget({ widget: "#22", container: "#20", index: 3 })).toBe(
            false
        );
        expect(harness.removeChildWidget({ widget: "#22", container: "#20" })).toBe(true);
        expect(childIds(harness.widgets["#20"])).toEqual(["#21"]);
    });
});
