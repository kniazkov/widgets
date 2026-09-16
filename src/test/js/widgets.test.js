import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(
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

describe("text input alignment", () => {
    it("applies every horizontal alignment supported by the protocol", () => {
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.eval(`${source}\nwindow.__initTextAlignment = initTextAlignment;`);

        const input = dom.window.document.createElement("input");
        dom.window.__initTextAlignment(input);

        input._setHorzAlignment("center");
        expect(input.style.textAlign).toBe("center");
        input._setHorzAlignment("right");
        expect(input.style.textAlign).toBe("right");
        input._setHorzAlignment("justify");
        expect(input.style.textAlign).toBe("justify");
        input._setHorzAlignment("left");
        expect(input.style.textAlign).toBe("left");
    });
});

function createTextInputHarness(type) {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${librarySource}
        function sendEventToServer() {}
        ${source}
        const editable = widgetsLibrary[${JSON.stringify(type)}]();
        editable._id = "#1";
        widgets[editable._id] = editable;
        document.body.appendChild(editable);
        window.__textInputHarness = { editable, setText };
    `);
    return dom.window.__textInputHarness;
}

describe("delayed text model updates", () => {
    it.each(["input field", "password input", "text area"])(
        "does not overwrite local editing in a %s",
        type => {
            const harness = createTextInputHarness(type);
            const widget = harness.editable;
            widget.focus();
            widget.value = "abcdef";
            widget.dispatchEvent(new dom.window.Event("input"));

            expect(harness.setText({ widget: "#1", text: "a" })).toBe(true);
            expect(widget.value).toBe("abcdef");

            widget.blur();
            expect(widget.value).toBe("abcdef");

            expect(harness.setText({ widget: "#1", text: "ABCDEF" })).toBe(true);
            expect(widget.value).toBe("abcdef");
            widget._textInputPending = false;
            widget._applyDeferredText();

            expect(widget.value).toBe("ABCDEF");
        }
    );

    it.each(["input field", "password input", "text area"])(
        "applies a deferred %s update after focus leaves",
        type => {
            const harness = createTextInputHarness(type);
            const widget = harness.editable;
            widget.focus();
            widget.value = "local";

            expect(harness.setText({ widget: "#1", text: "server" })).toBe(true);
            expect(widget.value).toBe("local");

            widget.blur();

            expect(widget.value).toBe("server");
        }
    );
});


function createOverlayStackHarness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${librarySource}
        function sendEventToServer() {}
        ${source}
        const stack = widgetsLibrary["overlay stack"]();
        stack._id = "#1";
        widgets[stack._id] = stack;
        document.body.appendChild(stack);
        window.__overlayStackHarness = {
            stack,
            append(child, id) {
                child._id = id;
                widgets[id] = child;
                return appendChildWidget({
                    widget: id,
                    container: stack._id
                });
            }
        };
    `);
    return dom.window.__overlayStackHarness;
}

describe("overlay stack", () => {
    it("places appended children into one grid cell in layer order", () => {
        const harness = createOverlayStackHarness();
        const bottom = dom.window.document.createElement("span");
        const top = dom.window.document.createElement("img");

        expect(harness.append(bottom, "#2")).toBe(true);
        expect(harness.append(top, "#3")).toBe(true);

        expect(harness.stack.style.display).toBe("inline-grid");
        expect(bottom.style.gridArea).toBe("1 / 1");
        expect(top.style.gridArea).toBe("1 / 1");
        expect(harness.stack.children[0]).toBe(bottom);
        expect(harness.stack.children[1]).toBe(top);
    });
});
