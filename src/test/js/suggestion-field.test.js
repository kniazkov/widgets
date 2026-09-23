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
            setSuggestions,
            setText,
            removeChildWidget,
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

function field() {
    const harness = createHarness();
    harness.createWidget({ type: "suggestion field", widget: "#30" });
    const input = harness.widgets["#30"];
    dom.window.document.body.appendChild(input);
    harness.setSuggestions({ widget: "#30", suggestions: ["Silver", "Rhodium brass", "Gold"] });
    return { harness, input, document: dom.window.document };
}

function type(input, value) {
    input.value = value;
    input.dispatchEvent(new dom.window.Event("input"));
}

function key(input, value) {
    input.dispatchEvent(new dom.window.KeyboardEvent("keydown", { key: value, bubbles: true }));
}

function options(document) {
    return [...document.querySelectorAll('[role="option"]')].map(el => el.textContent);
}

describe("suggestion field", () => {
    it("shows all suggestions on empty focus and filters case-insensitive substrings", () => {
        const { input, document } = field();
        input.focus();
        expect(options(document)).toEqual(["Silver", "Rhodium brass", "Gold"]);
        type(input, "DIUM");
        expect(options(document)).toEqual(["Rhodium brass"]);
        type(input, "new value");
        expect(options(document)).toEqual([]);
        expect(input.value).toBe("new value");
        expect(input.getAttribute("aria-expanded")).toBe("false");
    });

    it("selects with keyboard through the text event pipeline without adding history", () => {
        const { harness, input, document } = field();
        input.focus();
        key(input, "ArrowUp");
        expect(document.querySelector('[aria-selected="true"]').textContent).toBe("Gold");
        key(input, "Enter");
        expect(input.value).toBe("Gold");
        expect(harness.events.filter(event => event.type === "text input").at(-1).data.text).toBe(
            "Gold"
        );
        expect(input._suggestions).toEqual(["Silver", "Rhodium brass", "Gold"]);
        expect(document.querySelector('[role="listbox"]')).toBeNull();
        expect(document.activeElement).toBe(input);
        type(input, "Gold alloy");
        expect(input.value).toBe("Gold alloy");
    });

    it("clicks literal text safely and handles reactive suggestions without losing text", () => {
        const { harness, input, document } = field();
        harness.setSuggestions({ widget: "#30", suggestions: ["<img src=x>", "", "<img src=x>"] });
        input.focus();
        expect(options(document)).toEqual(["<img src=x>"]);
        expect(document.querySelector("img")).toBeNull();
        document.querySelector('[role="option"]').click();
        expect(input.value).toBe("<img src=x>");
        type(input, "Other");
        harness.setSuggestions({ widget: "#30", suggestions: ["Other choice"] });
        expect(options(document)).toEqual(["Other choice"]);
        expect(input.value).toBe("Other");
        harness.setSuggestions({ widget: "#30", suggestions: [] });
        expect(options(document)).toEqual([]);
    });

    it("dismisses on escape, tab, blur, outside pointer and disabling", () => {
        const { harness, input, document } = field();
        input.focus();
        key(input, "ArrowDown");
        key(input, "Escape");
        expect(input.value).toBe("");
        expect(options(document)).toEqual([]);
        key(input, "ArrowDown");
        key(input, "Tab");
        expect(options(document)).toEqual([]);
        input.click();
        document.body.dispatchEvent(new dom.window.Event("pointerdown", { bubbles: true }));
        expect(options(document)).toEqual([]);
        input.click();
        input.blur();
        expect(options(document)).toEqual([]);
        input.focus();
        harness.setDisabledFlag({ widget: "#30", disabled: true });
        expect(options(document)).toEqual([]);
        input.click();
        expect(options(document)).toEqual([]);
    });

    it("removes the portal when an ancestor is removed", async () => {
        const { input, document } = field();
        const parent = document.createElement("div");
        document.body.appendChild(parent);
        parent.appendChild(input);
        input.focus();
        parent.remove();
        await Promise.resolve();
        expect(document.querySelector('[role="listbox"]')).toBeNull();
    });

    it("does not intercept an IME composition or delayed server text echo", () => {
        const { harness, input, document } = field();
        input.focus();
        input.dispatchEvent(new dom.window.Event("compositionstart"));
        type(input, "sil");
        key(input, "ArrowDown");
        key(input, "Enter");
        expect(input.value).toBe("sil");
        expect(options(document)).toEqual([]);
        input.dispatchEvent(new dom.window.Event("compositionend"));
        expect(options(document)).toEqual(["Silver"]);
        harness.setText({ widget: "#30", text: "stale" });
        expect(input.value).toBe("sil");
    });
});
