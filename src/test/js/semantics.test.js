import { readScriptGroup } from "./helpers/scripts.js";
import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

let dom;

afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`
        ${readScriptGroup("shared")}
        ${readScriptGroup("widgets")}
        window.__semantics = {
            createWidget, widgets, appendChildWidget, removeChildWidget, disposeWidget,
            setAccessibleReference, setWidgetAccessibleName, setWidgetAccessibleDescription,
            setHeadingLevel, setAlternativeText, setDecorativeImage, setValidFlag
        };
    `);
    return dom.window.__semantics;
}

describe("shared accessibility protocol", () => {
    it("tracks live ID relationships through attachment, detach and permanent disposal", () => {
        const h = harness();
        for (const [widget, type] of [
            ["#1", "root"],
            ["#2", "input field"],
            ["#3", "text"],
            ["#4", "text"],
            ["#5", "text"]
        ]) {
            expect(h.createWidget({ widget, type })).toBe(true);
        }
        const put = widget => h.appendChildWidget({ widget, container: "#1" });
        const source = h.widgets["#2"];
        put("#2");
        const set = (key, id) => h.setAccessibleReference({ widget: "#2", [key]: id }, key);
        expect(set("accessible label", "#3")).toBe(true);
        expect(set("accessible description widget", "#4")).toBe(true);
        expect(set("accessible error", "#5")).toBe(true);
        expect(set("accessible label", "#2")).toBe(false);
        expect(source.getAttribute("aria-labelledby")).toBeNull();
        put("#3");
        put("#4");
        put("#5");
        const labelId = h.widgets["#3"].id;
        expect(source.getAttribute("aria-labelledby")).toBe(labelId);
        expect(source.getAttribute("aria-describedby")).toBe(h.widgets["#4"].id);
        expect(source.getAttribute("aria-errormessage")).toBeNull();
        h.setValidFlag({ widget: "#2", valid: false });
        expect(source.getAttribute("aria-invalid")).toBe("true");
        expect(source.getAttribute("aria-errormessage")).toBe(h.widgets["#5"].id);
        expect(source.getAttribute("aria-describedby")).toBe(
            h.widgets["#4"].id + " " + h.widgets["#5"].id
        );
        h.removeChildWidget({ widget: "#3", container: "#1" });
        expect(source.getAttribute("aria-labelledby")).toBeNull();
        put("#3");
        expect(source.getAttribute("aria-labelledby")).toBe(labelId);
        h.disposeWidget({ widget: "#3" });
        expect(source.getAttribute("aria-labelledby")).toBeNull();
        h.setValidFlag({ widget: "#2", valid: true });
        expect(source.getAttribute("aria-errormessage")).toBeNull();
        expect(source.getAttribute("aria-invalid")).toBeNull();
        expect(source.getAttribute("aria-describedby")).toBe(h.widgets["#4"].id);
    });

    it("uses text-only metadata, heading levels and reversible decorative state", () => {
        const h = harness();
        const literal = 'A <span> "quoted" & value';
        for (const [widget, type] of [
            ["#1", "button"],
            ["#2", "image"],
            ["#3", "text"]
        ]) {
            h.createWidget({ widget, type });
        }
        h.setWidgetAccessibleName({ widget: "#1", "accessible name": literal });
        h.setWidgetAccessibleDescription({ widget: "#1", "accessible description": literal });
        expect(h.widgets["#1"].getAttribute("aria-label")).toBe(literal);
        expect(h.widgets["#1"].getAttribute("aria-description")).toBe(literal);
        expect(h.widgets["#1"].querySelector("span")).toBeNull();
        h.setAlternativeText({ widget: "#2", "alternative text": literal });
        expect(h.widgets["#2"].alt).toBe(literal);
        h.setDecorativeImage({ widget: "#2", "decorative image": true });
        expect(h.widgets["#2"].alt).toBe("");
        expect(h.widgets["#2"].getAttribute("aria-hidden")).toBe("true");
        h.setDecorativeImage({ widget: "#2", "decorative image": false });
        expect(h.widgets["#2"].alt).toBe(literal);
        h.setHeadingLevel({ widget: "#3", "heading level": 3 });
        expect(h.widgets["#3"].getAttribute("role")).toBe("heading");
        expect(h.widgets["#3"].getAttribute("aria-level")).toBe("3");
        h.setHeadingLevel({ widget: "#3", "heading level": 0 });
        expect(h.widgets["#3"].getAttribute("role")).toBeNull();
        expect(h.widgets["#3"].getAttribute("aria-level")).toBeNull();
    });
});
