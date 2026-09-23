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
            setSuggestionSeparator,
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

describe("separated suggestions", () => {
    function separated(separator = ",") {
        const state = field();
        state.harness.setSuggestionSeparator({ widget: "#30", "suggestion separator": separator });
        state.input.focus();
        return state;
    }

    it("replaces the last token and sends the entire text to the server", () => {
        const { input, harness, document } = separated();
        type(input, "Silver, go");
        expect(options(document)).toEqual(["Gold"]);
        key(input, "ArrowDown");
        key(input, "Enter");
        expect(input.value).toBe("Silver, Gold");
        expect(input.selectionStart).toBe(12);
        expect(harness.events.at(-1).data.text).toBe("Silver, Gold");
        expect(options(document)).toEqual([]);
    });

    it("edits the middle token preserving whitespace and both neighbours", () => {
        const { input, document } = separated();
        type(input, "Silver,  go  , Custom");
        input.setSelectionRange(10, 10);
        input.click();
        expect(options(document)).toEqual(["Gold"]);
        document.querySelector('[role="option"]').click();
        expect(input.value).toBe("Silver,  Gold  , Custom");
        expect(input.selectionStart).toBe(13);
        input.setSelectionRange(2, 2);
        input.dispatchEvent(new dom.window.KeyboardEvent("keyup", { key: "Home" }));
        expect(options(document)).toEqual(["Silver"]);
    });

    it("handles empty trailing tokens and literal multi-character separators", () => {
        const { input, document } = separated("||");
        type(input, "Silver||  ");
        expect(options(document)).toEqual(["Rhodium brass", "Gold"]);
        document.querySelectorAll('[role="option"]')[1].click();
        expect(input.value).toBe("Silver||  Gold");
        input.setSelectionRange(7, 7);
        input.click();
        expect(options(document)).toEqual([]);
    });

    it("does not offer replacement across a separator or during composition", () => {
        const { input, document } = separated();
        type(input, "Silver, Gold");
        input.setSelectionRange(2, 10);
        input.click();
        expect(options(document)).toEqual([]);
        input.dispatchEvent(new dom.window.Event("compositionstart"));
        type(input, "Silver, go");
        key(input, "ArrowDown");
        key(input, "Enter");
        expect(input.value).toBe("Silver, go");
        expect(options(document)).toEqual([]);
        input.dispatchEvent(new dom.window.Event("compositionend"));
        expect(options(document)).toEqual(["Gold"]);
    });

    it("hides other tokens case-insensitively but allows editing the current token", () => {
        const { input, document } = separated();
        type(input, " silver , ");
        expect(options(document)).toEqual(["Rhodium brass", "Gold"]);
        input.setSelectionRange(3, 3);
        input.click();
        expect(options(document)).toEqual(["Silver"]);
    });

    it("removes typed or pasted duplicates on blur, preserving first spelling and order", () => {
        const { input, harness } = separated();
        type(input, "Silver, SILVER , Gold, silver");
        expect(input.value).toBe("Silver, SILVER , Gold, silver");
        input.blur();
        expect(input.value).toBe("Silver, Gold");
        expect(harness.events.at(-1).data.text).toBe("Silver, Gold");
        input.focus();
        type(input, "Custom, custom, Other");
        input.blur();
        expect(input.value).toBe("Custom, Other");
    });

    it("waits for composition to end after blur and does not rewrite disabled fields", () => {
        const { input, harness } = separated();
        input.dispatchEvent(new dom.window.Event("compositionstart"));
        type(input, "Gold, gold");
        input.blur();
        expect(input.value).toBe("Gold, gold");
        input.dispatchEvent(new dom.window.Event("compositionend"));
        expect(input.value).toBe("Gold");
        input.focus();
        type(input, "Silver, Silver");
        harness.setDisabledFlag({ widget: "#30", disabled: true });
        input.blur();
        expect(input.value).toBe("Silver, Silver");
    });

    it("preserves single-value input and supports duplicate removal with literal separators", () => {
        const { input, harness } = separated("||");
        type(input, "Gold||gold||Silver");
        input.blur();
        expect(input.value).toBe("Gold||Silver");
        harness.setSuggestionSeparator({ widget: "#30", "suggestion separator": "" });
        input.focus();
        type(input, "Gold, Gold");
        input.blur();
        expect(input.value).toBe("Gold, Gold");
    });

    it("reacts to separator and source updates without changing the text", () => {
        const { input, harness, document } = separated();
        type(input, "Silver; go");
        expect(options(document)).toEqual([]);
        harness.setSuggestionSeparator({ widget: "#30", "suggestion separator": ";" });
        expect(options(document)).toEqual(["Gold"]);
        harness.setSuggestions({ widget: "#30", suggestions: ["Gold alloy", "Gold; Silver"] });
        expect(options(document)).toEqual(["Gold alloy"]);
        harness.setSuggestionSeparator({ widget: "#30", "suggestion separator": "" });
        expect(options(document)).toEqual([]);
        expect(input.value).toBe("Silver; go");
        expect(
            harness.setSuggestionSeparator({ widget: "#30", "suggestion separator": null })
        ).toBe(false);
    });
});
