import { JSDOM } from "jsdom";
import { afterEach, expect, it } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

let dom;

afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`
        const observations = [];
        let frame;
        window.requestAnimationFrame = callback => { frame = callback; return 1 };
        window.cancelAnimationFrame = () => { frame = null };
        class ResizeObserver {
            constructor(callback) {
                this.resize = width => {
                    callback([{ contentRect: { width } }]);
                    const next = frame;
                    frame = null;
                    next?.();
                };
                this.active = false;
                observations.push(this);
            }
            observe() { this.active = true; }
            disconnect() { this.active = false; }
        }
        function sendEventToServer() {}
        ${readScriptGroup("shared")}
        ${readScriptGroup("widgets")}
        window.api = { observations, widgets, createWidget, setAdaptiveLayout, retainDetachedWidget,
            resumeWidgetSubtree };
    `);
    const api = dom.window.api;
    function add(id, type, parent = dom.window.document.body) {
        expect(api.createWidget({ widget: id, type })).toBe(true);
        const widget = api.widgets[id];
        parent.append(widget);
        api.resumeWidgetSubtree(widget);
        return widget;
    }
    return {
        ...api,
        add,
        set: (id, key, value) => api.setAdaptiveLayout({ widget: id, [key]: value }, key)
    };
}

it("changes stack and grid properties in-place without replacing ordered children", () => {
    const h = harness();
    const stack = h.add("#1", "stack");
    const grid = h.add("#2", "responsive grid", stack);
    const child = h.add("#3", "input field", grid);
    child.focus();
    expect(h.set("#1", "stack direction", "horizontal")).toBe(true);
    expect(h.set("#1", "stack alignment", "center")).toBe(true);
    expect(h.set("#1", "stack wrap", false)).toBe(true);
    expect(h.set("#1", "layout gap", 8)).toBe(true);
    expect(stack.hasAttribute("data-inline-axis")).toBe(true);
    expect(stack.hasAttribute("data-no-wrap")).toBe(true);
    expect(stack.style.alignItems).toBe("center");
    expect(h.set("#2", "grid min width", 130)).toBe(true);
    expect(h.set("#2", "grid max width", 240)).toBe(true);
    expect(grid.style.getPropertyValue("--widgets-grid-min")).toBe("130px");
    expect(grid.style.getPropertyValue("--widgets-grid-max")).toContain("240px");
    expect(h.set("#1", "grid min width", 200)).toBe(false);
    expect(h.set("#2", "layout gap", -1)).toBe(false);
    expect(h.set("#2", "grid max width", 0)).toBe(true);
    expect(grid.style.getPropertyValue("--widgets-grid-max")).toBe("1fr");
    expect(grid.firstElementChild).toBe(child);
    expect(dom.window.document.activeElement).toBe(child);
});

it("responds to own width and updated breakpoints and stops measuring while detached", () => {
    const h = harness();
    const form = h.add("#1", "form layout");
    const child = h.add("#2", "input field", form);
    const [observer] = h.observations;
    child.focus();
    expect(observer.active).toBe(true);
    observer.resize(500);
    expect(form.hasAttribute("data-label-columns")).toBe(false);
    expect(h.set("#1", "form breakpoint", 490)).toBe(true);
    expect(form.hasAttribute("data-label-columns")).toBe(true);
    observer.resize(489);
    expect(form.hasAttribute("data-label-columns")).toBe(false);
    expect(dom.window.document.activeElement).toBe(child);
    h.retainDetachedWidget(form);
    expect(observer.active).toBe(false);
    observer.resize(700);
    expect(form.hasAttribute("data-label-columns")).toBe(false);
    dom.window.document.body.append(form);
    h.resumeWidgetSubtree(form);
    expect(observer.active).toBe(true);
    observer.resize(700);
    expect(form.hasAttribute("data-label-columns")).toBe(true);
    expect(form.firstElementChild).toBe(child);
});
