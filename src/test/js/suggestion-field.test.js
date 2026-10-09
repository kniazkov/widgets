import { readScript, readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const optionsSource = readScript("options.js");
const librarySource = readScriptGroup("shared");
const widgetsSource = readScriptGroup("widgets");

let dom;

afterEach(() => {
    dom?.window.document.activeElement?.blur();
    dom?.window.close();
});

function createHarness(visualClientCoordinates = false) {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    const frames = new Map();
    let frameId = 0;
    dom.window.requestAnimationFrame = callback => {
        frames.set(++frameId, callback);
        return frameId;
    };
    dom.window.cancelAnimationFrame = id => frames.delete(id);
    dom.window.CSS = { supports: () => visualClientCoordinates };
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
            setPlaceholder,
            setPlaceholderColor,
            setInputMode,
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
        events: dom.window.__selectionEvents,
        frames,
        nextFrame() {
            const callbacks = [...frames.values()];
            frames.clear();
            callbacks.forEach(callback => callback());
        }
    };
}

function field(visualClientCoordinates = false) {
    const harness = createHarness(visualClientCoordinates);
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

function touchEvent(target, type, { x = 10, y = 10, extra = false } = {}) {
    const point = { identifier: 1, clientX: x, clientY: y, target };
    const event = new dom.window.Event(type, { bubbles: true, cancelable: true });
    Object.assign(event, {
        changedTouches: [point],
        touches:
            type === "touchend" || type === "touchcancel"
                ? []
                : extra
                  ? [point, { identifier: 2, clientX: 20, clientY: 20, target }]
                  : [point]
    });
    target.dispatchEvent(event);
    return event;
}

describe("suggestion field", () => {
    it("updates input hints without touching value, caret or validity", () => {
        const { harness, input } = field();
        input.focus();
        type(input, "12.50");
        input.setSelectionRange(2, 2);
        expect(harness.setPlaceholder({ widget: "#30", placeholder: "0.00" })).toBe(true);
        expect(harness.setInputMode({ widget: "#30", "input mode": "decimal" })).toBe(true);
        expect(
            harness.setPlaceholderColor({
                widget: "#30",
                "placeholder color": { r: 80, g: 90, b: 100 }
            })
        ).toBe(true);
        expect(input.style.getPropertyValue("--widgets-placeholder-color")).toBe("rgb(80,90,100)");
        expect(harness.setPlaceholderColor({ widget: "#30", "placeholder color": null })).toBe(
            false
        );
        expect(input.placeholder).toBe("0.00");
        expect(input.inputMode).toBe("decimal");
        expect(input.value).toBe("12.50");
        expect(input.selectionStart).toBe(2);
        expect(harness.setInputMode({ widget: "#30", "input mode": "invalid" })).toBe(false);
        expect(harness.setPlaceholder({ widget: "#30", placeholder: null })).toBe(false);
        harness.setPlaceholder({ widget: "#30", placeholder: "" });
        expect(input.placeholder).toBe("");
    });

    it("selects on touchend even if Safari blurs before click", () => {
        const { harness, input, document } = field();
        const value = "Латунь с родиевым покрытием";
        harness.setSuggestions({ widget: "#30", suggestions: [value] });
        Object.defineProperty(input, "scrollWidth", { value: 600 });
        input.focus();
        const option = document.querySelector('[role="option"]');
        touchEvent(option, "touchstart");
        input.blur();
        // A real browser cannot click an option removed during blur.
        expect(option.isConnected).toBe(true);
        expect(input.value).toBe("");
        expect(touchEvent(option, "touchend").defaultPrevented).toBe(true);
        option.click(); // A late compatibility click must not select twice.
        expect(document.activeElement).toBe(input);
        expect(input.value).toBe(value);
        expect(input.selectionStart).toBe(value.length);
        expect(input.selectionEnd).toBe(value.length);
        expect(input.scrollLeft).toBe(600);
        expect(options(document)).toEqual([]);
        expect(harness.events.filter(event => event.type === "text input")).toHaveLength(1);
        harness.setText({ widget: "#30", text: "stale" });
        expect(input.selectionStart).toBe(value.length);
        expect(input.value).toBe(value);
    });

    it.each(["touchcancel", "touchmove", "pointercancel", "multitouch"])(
        "does not select during touch scrolling or cancellation (%s)",
        cancellation => {
            const { harness, input, document } = field();
            input.focus();
            const option = document.querySelector('[role="option"]');
            touchEvent(option, "touchstart");
            input.blur();
            if (cancellation === "multitouch") touchEvent(option, "touchstart", { extra: true });
            else if (cancellation === "pointercancel") {
                option.dispatchEvent(new dom.window.Event("pointercancel", { bubbles: true }));
            } else touchEvent(option, cancellation, { y: 50 });
            // Returning to the start must not turn a drag into a tap.
            touchEvent(option, "touchend");
            expect(input.value).toBe("");
            expect(options(document)).toEqual([]);
            expect(harness.events.filter(event => event.type === "text input")).toHaveLength(0);
        }
    );

    it("keeps native scrolling available and ignores detached touch targets", () => {
        const { harness, input, document } = field();
        input.focus();
        const option = document.querySelector('[role="option"]');
        expect(touchEvent(option, "touchstart").defaultPrevented).toBe(false);
        expect(touchEvent(option, "touchmove", { y: 50 }).defaultPrevented).toBe(false);
        harness.setDisabledFlag({ widget: "#30", disabled: true });
        touchEvent(option, "touchend");
        option.click();
        expect(input.value).toBe("");
        expect(options(document)).toEqual([]);
    });

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

    it("reopens on a touch pointer release in an already focused field without a click", () => {
        const { input, document, harness } = field();
        input.focus();
        key(input, "Escape");
        expect(document.activeElement).toBe(input);
        expect(options(document)).toEqual([]);
        const event = new dom.window.Event("pointerup");
        Object.defineProperty(event, "pointerType", { value: "touch" });
        input.dispatchEvent(event);
        expect(options(document)).toEqual(["Silver", "Rhodium brass", "Gold"]);
        expect(harness.events.filter(event => event.type === "text input")).toHaveLength(0);
        input.blur();
        input.dispatchEvent(event);
        expect(options(document)).toEqual([]);
        expect(document.activeElement).not.toBe(input);
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

describe("suggestion positioning", () => {
    function geometry(visualClientCoordinates = false) {
        const state = field(visualClientCoordinates);
        const viewport = new dom.window.EventTarget();
        Object.assign(viewport, { offsetTop: 0, offsetLeft: 0, width: 390, height: 700 });
        Object.defineProperty(dom.window, "visualViewport", { value: viewport });
        const anchor = { top: 180, bottom: 220, left: 20, right: 260, width: 240 };
        function clientRect(rect) {
            const x = visualClientCoordinates ? viewport.offsetLeft : 0;
            const y = visualClientCoordinates ? viewport.offsetTop : 0;
            return {
                ...rect,
                top: rect.top - y,
                bottom: rect.bottom - y,
                left: rect.left - x,
                right: rect.right - x
            };
        }
        state.input.getBoundingClientRect = () => clientRect(anchor);
        state.input.focus();
        const list = state.document.querySelector('[role="listbox"]');
        // Simulate a fixed/top-layer origin drifting independently of the input.
        const origin = { x: 0, y: 0 };
        list.getBoundingClientRect = () => {
            const height = Math.min(132, parseFloat(list.style.maxHeight));
            const top =
                parseFloat(list.style.top || 0) + origin.y - (list.style.transform ? height : 0);
            const left = parseFloat(list.style.left || 0) + origin.x;
            const width = parseFloat(list.style.width);
            return clientRect({
                top,
                bottom: top + height,
                left,
                right: left + width,
                width,
                height
            });
        };
        state.harness.nextFrame();
        return { ...state, viewport, anchor, list, origin };
    }

    for (const visual of [false, true]) {
        describe(visual ? "visual client coordinates (iOS)" : "layout client coordinates", () => {
            it.each(["stationary", "upward", "downward"])(
                "keeps an empty focused field's suggestions visible during %s keyboard movement",
                movement => {
                    const { harness, input, anchor, viewport, list } = geometry(visual);
                    Object.assign(anchor, { top: 520, bottom: 560 });
                    const offsets =
                        movement === "stationary"
                            ? [400, 400]
                            : movement === "upward"
                              ? [0, 200, 400]
                              : [500, 450, 400];
                    for (const offsetTop of offsets) {
                        Object.assign(viewport, { offsetTop, height: 320 });
                        harness.nextFrame();
                    }
                    expect(input.value).toBe("");
                    expect(dom.window.document.activeElement).toBe(input);
                    expect(list.isConnected).toBe(true);
                    expect(list.style.visibility).toBe("visible");
                    expect(list.getBoundingClientRect().top).toBe(
                        input.getBoundingClientRect().bottom + 4
                    );
                }
            );

            it("clamps both axes after zoom/panning and restores after a hidden intermediate frame", () => {
                const { harness, input, anchor, viewport, list } = geometry(visual);
                Object.assign(viewport, {
                    offsetTop: 400,
                    offsetLeft: 120,
                    width: 190,
                    height: 280
                });
                Object.assign(anchor, { top: 10, bottom: 50, left: 130, right: 370 });
                harness.nextFrame();
                expect(list.style.visibility).toBe("hidden");
                Object.assign(anchor, { top: 630, bottom: 670 });
                harness.nextFrame();
                expect(list.style.visibility).toBe("visible");
                const bounds = list.getBoundingClientRect();
                expect(bounds.bottom).toBe(input.getBoundingClientRect().top - 4);
                expect(bounds.top).toBeGreaterThanOrEqual(visual ? 4 : 404);
                expect(bounds.left).toBeGreaterThanOrEqual(visual ? 4 : 124);
                expect(bounds.right).toBeLessThanOrEqual(visual ? 186 : 306);
                key(input, "Escape");
                viewport.dispatchEvent(new dom.window.Event("scroll"));
                harness.nextFrame();
                expect(list.isConnected).toBe(false);
                expect(harness.frames.size).toBe(0);
            });
        });
    }

    it("follows late field movement and a drifting fixed origin without scroll events", () => {
        const { harness, anchor, list, origin } = geometry();
        anchor.top += 90;
        anchor.bottom += 90;
        origin.y = -55;
        origin.x = 10;
        harness.nextFrame();
        expect(list.getBoundingClientRect().top).toBe(anchor.bottom + 4);
        expect(list.getBoundingClientRect().left).toBe(anchor.left);
        harness.nextFrame();
        expect(list.getBoundingClientRect().top).toBe(anchor.bottom + 4);
        expect(harness.frames.size).toBe(1);
    });

    it("fits above or below the input inside a panned and resized visual viewport", () => {
        const { harness, anchor, viewport, list } = geometry();
        Object.assign(viewport, { offsetTop: 100, offsetLeft: 30, width: 220, height: 250 });
        Object.assign(anchor, { top: 300, bottom: 340 });
        viewport.dispatchEvent(new dom.window.Event("resize"));
        harness.nextFrame();
        const above = list.getBoundingClientRect();
        expect(above.bottom).toBe(anchor.top - 4);
        expect(above.top).toBeGreaterThanOrEqual(104);
        expect(above.left).toBeGreaterThanOrEqual(34);
        expect(above.right).toBeLessThanOrEqual(246);
        Object.assign(anchor, { top: 120, bottom: 160 });
        viewport.dispatchEvent(new dom.window.Event("scroll"));
        harness.nextFrame();
        const below = list.getBoundingClientRect();
        expect(below.top).toBe(anchor.bottom + 4);
        expect(below.bottom).toBeLessThanOrEqual(346);
        Object.assign(anchor, { top: 500, bottom: 540 });
        harness.nextFrame();
        expect(list.style.visibility).toBe("hidden");
        Object.assign(anchor, { top: 120, bottom: 160 });
        harness.nextFrame();
        expect(list.style.visibility).not.toBe("hidden");
    });

    it("stops tracking on close or detach and starts only one loop on reopening", async () => {
        const { harness, input, document } = geometry();
        key(input, "Escape");
        expect(harness.frames.size).toBe(0);
        input.click();
        input.click();
        expect(harness.frames.size).toBe(1);
        input.remove();
        await Promise.resolve();
        expect(harness.frames.size).toBe(0);
        expect(document.querySelector('[role="listbox"]')).toBeNull();
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
