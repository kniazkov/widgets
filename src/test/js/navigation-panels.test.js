import { JSDOM } from "jsdom";
import { expect, it } from "vitest";
import { readScript } from "./helpers/scripts.js";

function fixture(kind = "tabs") {
    const dom = new JSDOM("<!doctype html><body></body>", { runScripts: "outside-only" });
    dom.window.eval(`
        const widgets = {};
        function listenWidget(node, type, fn) {
            node._listenerScope ??= new AbortController();
            node.addEventListener(type, fn, {signal: node._listenerScope.signal});
        }
        function placeWidget(node, host) { host.append(node); }
        ${readScript("widgets/navigation.js")}
        window.api = {widgets, setNavigation, createBreadcrumbs};
    `);
    const { widgets, setNavigation } = dom.window.api;
    const document = dom.window.document;
    function node(id, tag, parent) {
        const element = document.createElement(tag);
        element._id = id;
        widgets[id] = element;
        parent.append(element);
        return element;
    }
    const root = node("root", "div", document.body);
    const row = node("row", "div", root);
    const state = { kind, headers: "row", items: [] };
    for (const key of ["a", "b", "c"]) {
        const headers = kind === "tabs" ? row : node("row-" + key, "div", root);
        node(key, "button", headers).textContent = key;
        const panel = node("panel-" + key, "div", root);
        node("input-" + key, "input", panel).value = "draft " + key;
        state.items.push({
            key,
            button: key,
            panel: panel._id,
            headers: headers._id,
            visible: true,
            enabled: true,
            selected: key === "a"
        });
    }
    const apply = () => setNavigation({ widget: "root", snapshot: structuredClone(state) });
    expect(apply()).toBe(true);
    function press(key) {
        document.activeElement.dispatchEvent(
            new dom.window.KeyboardEvent("keydown", { key, bubbles: true, cancelable: true })
        );
    }
    return { dom, document, widgets, root, state, apply, press };
}

it("moves roving focus without activation and skips unavailable headers", () => {
    const f = fixture();
    try {
        f.state.items[1].enabled = false;
        f.apply();
        f.widgets.a.focus();
        f.press("ArrowRight");
        expect(f.document.activeElement).toBe(f.widgets.c);
        expect(f.widgets.a.getAttribute("aria-selected")).toBe("true");
        f.press("Home");
        expect(f.document.activeElement).toBe(f.widgets.a);
        f.press("End");
        expect(f.document.activeElement).toBe(f.widgets.c);
        f.state.items[2].visible = false;
        f.apply();
        expect(f.document.activeElement).toBe(f.widgets.a);
    } finally {
        f.dom.window.close();
    }
});

it("preserves focused descendants and draft on reorder, recovers focus on removal", () => {
    const f = fixture();
    try {
        f.widgets["input-a"].focus();
        f.state.items.reverse();
        f.apply();
        expect(f.document.activeElement).toBe(f.widgets["input-a"]);
        expect(f.widgets["input-a"].value).toBe("draft a");
        f.state.items = f.state.items.filter(item => item.key !== "a");
        f.state.items[0].selected = true;
        f.apply();
        expect(f.document.activeElement).toBe(f.widgets.c);
        f.state.items = [];
        f.apply();
        expect(f.document.activeElement).toBe(f.root);
    } finally {
        f.dom.window.close();
    }
});

it("rejects partial, duplicate and foreign snapshots atomically", () => {
    const f = fixture();
    try {
        const before = f.root.outerHTML;
        f.state.items[1].button = "a";
        expect(f.apply()).toBe(false);
        expect(f.root.outerHTML).toBe(before);
        f.state.items[1].button = "b";
        f.state.items[0].selected = false;
        expect(f.apply()).toBe(false);
        expect(f.root.outerHTML).toBe(before);
        f.state.items[0].selected = true;
        f.document.body.append(f.widgets.b);
        expect(f.apply()).toBe(false);
    } finally {
        f.dom.window.close();
    }
});

it("accordion arrows preserve expansion; paused and disposed trees ignore keyboard events", () => {
    const f = fixture("accordion");
    try {
        f.widgets.a.focus();
        f.press("ArrowDown");
        expect(f.document.activeElement).toBe(f.widgets.b);
        expect(f.widgets.a.getAttribute("aria-expanded")).toBe("true");
        f.root._treePaused = true;
        f.press("End");
        expect(f.document.activeElement).toBe(f.widgets.b);
        f.root._treePaused = false;
        f.press("End");
        expect(f.document.activeElement).toBe(f.widgets.c);
        f.root._onDisposed();
        f.root._listenerScope.abort();
        f.press("Home");
        expect(f.document.activeElement).toBe(f.widgets.c);
        expect(f.root._navigation).toBeNull();
    } finally {
        f.dom.window.close();
    }
});

it("breadcrumbs keep actual anchors and update the unique current page", () => {
    const f = fixture();
    try {
        const path = f.dom.window.api.createBreadcrumbs();
        for (let i = 0; i < 3; i++) {
            const row = f.document.createElement("div");
            if (i < 2) {
                const link = f.document.createElement("a");
                link.href = "/";
                row.append(link);
            }
            path._childHost.append(row);
            path._layoutChild();
        }
        expect(path.querySelectorAll('[aria-current="page"]').length).toBe(1);
        expect(path.querySelectorAll('a[href="/"]').length).toBe(2);
        expect(path._childHost.lastElementChild.getAttribute("aria-current")).toBe("page");
    } finally {
        f.dom.window.close();
    }
});

it("moves focus out before replacing content without stealing unrelated focus", () => {
    const f = fixture();
    try {
        f.widgets["input-a"].focus();
        f.state.replacing = "a";
        f.apply();
        expect(f.document.activeElement).toBe(f.widgets.a);
        const outside = f.document.createElement("button");
        f.document.body.append(outside);
        outside.focus();
        f.apply();
        expect(f.document.activeElement).toBe(outside);
    } finally {
        f.dom.window.close();
    }
});
