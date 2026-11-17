import { JSDOM } from "jsdom";
import { afterEach, expect, it } from "vitest";
import { readScript } from "./helpers/scripts.js";

let current;
afterEach(() => {
    current?.widget._onDisposed();
    current?.dom.window.close();
});

function fixture(kind = "menu") {
    const dom = new JSDOM(
        "<!doctype html><body><button id='anchor'>Trigger</button><p id='help'>Existing help</p><input id='outside'></body>",
        { runScripts: "outside-only", pretendToBeVisual: true }
    );
    const window = dom.window;
    window.eval(`
        const widgets = {};
        let activeModal = null;
        const sent = [];
        function modalRoot(){return document.body;}
        function visibleModalElement(node){return !!node && node.isConnected && node.style.display !== "none";}
        function focusModalElement(node){if(!visibleModalElement(node)||node.disabled)return false;node.focus();return document.activeElement===node;}
        function modalTabStops(node){return Array.from(node.querySelectorAll('button,input')).filter(el=>!el.disabled);}
        function listenWidget(node,type,fn){node._listenerScope??=new AbortController();node.addEventListener(type,fn,{signal:node._listenerScope.signal});}
        function sendEventToServer(node,type,data){sent.push({type,...data});}
        class ResizeObserver {observe(){} disconnect(){}}
        const nativeMatches=Element.prototype.matches;
        Element.prototype.matches=function(selector){return selector===":popover-open"?!!this.opened:nativeMatches.call(this,selector);};
        HTMLElement.prototype.showPopover=function(){this.opened=true;};
        HTMLElement.prototype.hidePopover=function(){this.opened=false;};
        ${readScript("widgets/semantics.js")}
        ${readScript("widgets/anchored-overlays.js")}
        window.api={widgets,sent,anchoredPosition,createAnchoredOverlay,setAnchoredOverlay,
            refreshAnchoredOverlay,forgetAccessibleWidget,refreshAccessibleRelationsFor,
            setModal:value=>activeModal=value,overlayCount:()=>anchoredOverlays.size};
    `);
    const api = window.api;
    const anchor = window.document.getElementById("anchor");
    anchor._id = "anchor";
    anchor._accessibleReferences = { "accessible description widget": "help" };
    const help = window.document.getElementById("help");
    help._id = "help";
    const rect = { left: 50, top: 80, right: 150, bottom: 110, width: 100, height: 30 };
    anchor.getBoundingClientRect = () => rect;
    anchor.getClientRects = () => [rect];
    api.widgets.anchor = anchor;
    api.widgets.help = help;
    const widget = api.createAnchoredOverlay(kind);
    widget._id = "overlay";
    widget.getBoundingClientRect = () => ({
        width: 220,
        height: 160,
        left: 0,
        top: 0,
        right: 220,
        bottom: 160
    });
    const content = window.document.createElement("div");
    content._id = "content";
    widget.append(content);
    api.widgets.overlay = widget;
    api.widgets.content = content;
    window.document.body.append(widget);
    widget._onAttached();
    const state = {
        anchor: "anchor",
        content: "content",
        kind,
        placement: "bottom",
        open: false,
        revision: 1,
        showDelay: 20,
        hideDelay: 10,
        items:
            kind === "menu"
                ? [
                      { key: "a", label: "Action", enabled: true },
                      { key: "b", label: "Disabled", enabled: false },
                      { key: "c", label: "Last", enabled: true }
                  ]
                : []
    };
    const apply = () =>
        api.setAnchoredOverlay({ widget: "overlay", snapshot: structuredClone(state) });
    expect(apply()).toBe(true);
    current = { dom, widget, content, anchor, state, apply, ...api };
    return current;
}

it("flips all sides and clamps to shifted visual viewport, including a keyboard-sized viewport", () => {
    const f = fixture();
    const view = { left: 20, top: 100, width: 320, height: 240 };
    const a = { left: 280, top: 280, right: 330, bottom: 310 };
    expect(f.anchoredPosition(a, { width: 200, height: 120 }, view, "bottom")).toEqual({
        side: "top",
        x: 132,
        y: 154
    });
    expect(f.anchoredPosition(a, { width: 200, height: 120 }, view, "right").side).toBe("left");
    const b = { left: 25, top: 110, right: 60, bottom: 140 };
    expect(f.anchoredPosition(b, { width: 200, height: 120 }, view, "top").side).toBe("bottom");
    expect(f.anchoredPosition(b, { width: 200, height: 120 }, view, "left").side).toBe("right");
});

it("validates snapshots atomically and gates rapid choices by one pending command", () => {
    const f = fixture();
    f.anchor.click();
    f.anchor.click();
    expect(f.sent).toHaveLength(1);
    expect(f.sent[0]).toEqual({ type: "overlay command", action: "open", key: "", revision: 1 });
    f.state.open = true;
    f.state.revision = 2;
    f.apply();
    const button = f.widget.querySelector('[data-key="a"]');
    button.click();
    button.click();
    expect(f.sent).toHaveLength(2);
    expect(f.sent[1].action).toBe("choose");
    expect(f.widget.matches(":popover-open")).toBe(false);
    const before = f.widget.innerHTML;
    f.state.items.push({ ...f.state.items[0] });
    expect(f.apply()).toBe(false);
    expect(f.widget.innerHTML).toBe(before);
});

it("menu keys skip disabled actions and Escape is consumed ahead of a modal", () => {
    const f = fixture();
    f.state.open = true;
    f.state.revision = 2;
    f.apply();
    const doc = f.dom.window.document;
    expect(doc.activeElement.dataset.key).toBe("a");
    doc.activeElement.dispatchEvent(
        new f.dom.window.KeyboardEvent("keydown", {
            key: "ArrowDown",
            bubbles: true,
            cancelable: true
        })
    );
    expect(doc.activeElement.dataset.key).toBe("c");
    let propagated = 0;
    doc.body.addEventListener("keydown", () => propagated++);
    doc.activeElement.dispatchEvent(
        new f.dom.window.KeyboardEvent("keydown", {
            key: "Escape",
            bubbles: true,
            cancelable: true
        })
    );
    expect(propagated).toBe(0);
    expect(doc.activeElement).toBe(f.anchor);
    expect(f.sent.at(-1).action).toBe("close");
});

it("tooltip composes descriptions and unrelated disposal does not crash or erase help", () => {
    const f = fixture("tooltip");
    expect(f.anchor.getAttribute("aria-describedby")).toContain("help");
    expect(f.anchor.getAttribute("aria-describedby")).toContain(f.widget.id);
    f.forgetAccessibleWidget(f.dom.window.document.getElementById("outside"));
    expect(f.anchor.getAttribute("aria-describedby")).toContain(f.widget.id);
    f.widget._onDetached();
    expect(f.anchor.getAttribute("aria-describedby")).toBe("help");
    expect(f.overlayCount()).toBe(0);
});

it("hover cancellation prevents a late open and disposal removes anchor listeners", async () => {
    const f = fixture("tooltip");
    const pointer = type =>
        f.anchor.dispatchEvent(new f.dom.window.MouseEvent(type, { bubbles: false }));
    pointer("pointerenter");
    pointer("pointerleave");
    await new Promise(resolve => setTimeout(resolve, 35));
    expect(f.sent).toHaveLength(0);
    pointer("pointerenter");
    await new Promise(resolve => setTimeout(resolve, 35));
    expect(f.sent).toHaveLength(1);
    f.widget._onDetached();
    f.widget._onAttached();
    f.widget._onDisposed();
    pointer("pointerenter");
    await new Promise(resolve => setTimeout(resolve, 35));
    expect(f.sent).toHaveLength(1);
    expect(f.overlayCount()).toBe(0);
});

it("anchor removal closes the top layer and releases relationships without stealing outside focus", () => {
    const f = fixture();
    f.state.open = true;
    f.state.revision = 2;
    f.apply();
    const outside = f.dom.window.document.getElementById("outside");
    outside.focus();
    f.anchor.remove();
    f.refreshAnchoredOverlay(f.widget);
    expect(f.widget.matches(":popover-open")).toBe(false);
    expect(outside).toBe(f.dom.window.document.activeElement);
    expect(f.anchor.hasAttribute("aria-controls")).toBe(false);
});

it("accepts an in-flight acknowledgement after anchor removal and closes only once", () => {
    const f = fixture();
    f.anchor.click();
    f.anchor.remove();
    delete f.widgets.anchor;
    f.state.open = true;
    f.state.revision++;
    expect(f.apply()).toBe(true);
    f.refreshAnchoredOverlay(f.widget);
    f.refreshAnchoredOverlay(f.widget);
    expect(f.sent.filter(event => event.action === "close")).toHaveLength(1);
    f.state.open = false;
    f.state.revision++;
    expect(f.apply()).toBe(true);
    expect(f.widget.matches(":popover-open")).toBe(false);
});

it("restores a tooltip relationship after attaching its anchor later in the same batch", () => {
    const f = fixture("tooltip");
    f.anchor.remove();
    f.refreshAnchoredOverlay(f.widget);
    expect(f.anchor.getAttribute("aria-describedby") || "").not.toContain(f.widget.id);
    f.dom.window.document.body.append(f.anchor);
    f.refreshAnchoredOverlay(f.widget);
    expect(f.anchor.getAttribute("aria-describedby")).toContain(f.widget.id);
});

it("a disabled anchor can cancel an in-flight choice without repeating the cleanup", () => {
    const f = fixture();
    f.state.open = true;
    f.state.revision++;
    f.apply();
    f.widget.querySelector('[data-key="a"]').click();
    f.anchor.disabled = true;
    f.refreshAnchoredOverlay(f.widget);
    f.refreshAnchoredOverlay(f.widget);
    expect(f.sent.map(event => event.action)).toEqual(["choose", "close"]);
    f.state.open = false;
    f.state.revision++;
    expect(f.apply()).toBe(true);
    f.anchor.disabled = false;
    f.refreshAnchoredOverlay(f.widget);
    f.anchor.click();
    expect(f.sent.at(-1).action).toBe("open");
});
