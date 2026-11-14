import { JSDOM } from "jsdom";
import { afterEach, expect, it } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

let dom;
afterEach(() => dom?.window.close());

function harness(type = "input field") {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`
        const sent = [];
        function createEvent(widget, type, data) { sent.push({ type, data }); return true; }
        function sendSynchronizeRequest() {}
        function sendEventToServer(widget, type, data) {
            if (type === "text input") createEvent(widget, type, data);
        }
        ${readScriptGroup("shared")}
        ${readScriptGroup("widgets")}
        window.testAPI = { createWidget, widgets, setKeyboardShortcuts, setInputReadOnly,
            disposeWidget, sent };
    `);
    const api = dom.window.testAPI;
    api.createWidget({ widget: "#1", type });
    const widget = api.widgets["#1"];
    dom.window.document.body.append(widget);
    function key(key, options = {}, phase = "keydown") {
        const e = new dom.window.KeyboardEvent(phase, {
            key,
            bubbles: true,
            cancelable: true,
            ...options
        });
        widget.dispatchEvent(e);
        return e.defaultPrevented;
    }
    return {
        ...api,
        widget,
        key,
        set: (...shortcuts) => api.setKeyboardShortcuts({ widget: "#1", shortcuts })
    };
}

it("passes ordinary typing and Tab; queues only exact explicit key notifications after input", () => {
    const h = harness("password input");
    expect(h.set()).toBe(true);
    expect(h.key("Enter")).toBe(false);
    expect(h.set("DOWN:Enter:0000:1")).toBe(true);
    expect(h.key("a")).toBe(false);
    expect(h.key("Tab")).toBe(false);
    expect(h.key("Enter", { shiftKey: true })).toBe(false);
    h.widget.value = "fake input";
    h.widget.dispatchEvent(new dom.window.Event("input"));
    expect(h.key("Enter", { code: "NumpadEnter" })).toBe(true);
    expect(h.key("Enter", { repeat: true })).toBe(true);
    expect(h.key("Enter", {}, "keyup")).toBe(false);
    expect(h.sent).toEqual([
        { type: "text input", data: { text: "fake input" } },
        {
            type: "keyboard",
            data: {
                key: "Enter",
                phase: "DOWN",
                ctrl: false,
                alt: false,
                shift: false,
                meta: false
            }
        }
    ]);
});

it("suppresses composition and suggestions; removal, re-add and disposal do not duplicate", () => {
    const h = harness();
    expect(h.set("DOWN:Enter:0000:1", "UP:Enter:0000:0")).toBe(true);
    h.widget.dispatchEvent(new dom.window.CompositionEvent("compositionstart"));
    expect(h.key("Enter", { isComposing: true })).toBe(false);
    h.widget.dispatchEvent(new dom.window.CompositionEvent("compositionend"));
    h.key("Enter", {}, "keyup");
    h.widget._isSuggestionOpen = () => true;
    expect(h.key("Enter")).toBe(false);
    h.widget._isSuggestionOpen = () => false;
    h.key("Enter", {}, "keyup");
    expect(h.sent).toHaveLength(0);
    expect(h.set("DOWN:Enter:0000:1", "DOWN:Enter:0000:0")).toBe(false);
    h.key("Enter");
    expect(h.sent).toHaveLength(1);
    h.set();
    expect(h.key("Enter")).toBe(false);
    h.set("DOWN:Enter:0000:1");
    h.key("Enter");
    expect(h.sent).toHaveLength(2);
    h.disposeWidget({ widget: "#1" });
    expect(h.key("Enter")).toBe(false);
    expect(h.sent).toHaveLength(2);
});

it("keeps multiline unmodified and ignores readonly, disabled and detached fields", () => {
    const h = harness("text area");
    expect(h.set("DOWN:Enter:1000:1", "UP:Enter:1000:0")).toBe(true);
    expect(h.key("Enter")).toBe(false);
    expect(h.key("Enter", { ctrlKey: true })).toBe(true);
    expect(h.key("Enter", { ctrlKey: true }, "keyup")).toBe(false);
    h.setInputReadOnly({ widget: "#1", "read only": true });
    expect(h.key("Enter", { ctrlKey: true })).toBe(false);
    h.setInputReadOnly({ widget: "#1", "read only": false });
    h.widget.disabled = true;
    expect(h.key("Enter", { ctrlKey: true })).toBe(false);
    h.widget.disabled = false;
    h.widget.remove();
    expect(h.key("Enter", { ctrlKey: true })).toBe(false);
    expect(h.sent.map(event => event.data.phase)).toEqual(["DOWN", "UP"]);
});
