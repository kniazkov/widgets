import { JSDOM } from "jsdom";
import { afterEach, expect, test } from "vitest";
import { readScript, readScriptGroup } from "./helpers/scripts.js";
let dom;
afterEach(() => dom?.window.close());
function harness(type = "input field") {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "https://example.com/"
    });
    dom.window
        .eval(`${readScript("options.js")}\n${readScriptGroup("shared")}\n${readScriptGroup("widgets")}\n${readScriptGroup("client")}
        sendSynchronizeRequest = () => {};
        const timers = [];
        window.setTimeout = fn => { const t={fn,active:true}; timers.push(t); return t; };
        window.clearTimeout = t => { if(t) t.active=false; };
        createWidget({type:${JSON.stringify(type)},widget:'#1'});
        createWidget({type:'button',widget:'#2'});
        const editor=widgets['#1'], button=widgets['#2'];
        document.body.append(editor,button);
        editor.getClientRects = () => [{}];
        button._events.click = true;
        window.h = {editor,button,events,setText,setClientEventAction,setInputDebounce,sendEventToServer,
          disposeWidget, validLocalAction, pauseWidgetSubtree, resumeWidgetSubtree, setKeyboardShortcuts,
          tick: () => { for(const t of timers.splice(0)) if(t.active)t.fn(); },
          ack: (id,text) => { removeProcessedEvents('#'+id); if(text!==undefined)setText({widget:'#1',text}); reconcileTextInputs(); }};`);
    return dom.window.h;
}
function input(h, text, composing = false) {
    h.editor.value = text;
    h.editor.dispatchEvent(
        new dom.window.InputEvent("input", { bubbles: true, isComposing: composing })
    );
}
function composition(h, type) {
    h.editor.dispatchEvent(new dom.window.CompositionEvent(type, { bubbles: true }));
}

test("composition updates stay local; compositionend and final input submit once", () => {
    const h = harness();
    h.editor.focus();
    composition(h, "compositionstart");
    input(h, "n", true);
    composition(h, "compositionupdate");
    input(h, "你", true);
    h.setText({ widget: "#1", text: "old response" });
    expect(h.editor.value).toBe("你");
    expect(h.events).toHaveLength(0);
    composition(h, "compositionend");
    input(h, "你");
    expect(h.events.map(e => e.data.text)).toEqual(["你"]);
    h.editor.blur();
    h.ack(1, "你");
    expect(h.editor.value).toBe("你");
});

test("blur before compositionend does not send or overwrite intermediate text", () => {
    const h = harness();
    h.editor.focus();
    composition(h, "compositionstart");
    input(h, "あ", true);
    h.editor.blur();
    h.setText({ widget: "#1", text: "stale" });
    h.ack(0);
    expect(h.events).toHaveLength(0);
    expect(h.editor.value).toBe("あ");
    composition(h, "compositionend");
    input(h, "あ");
    expect(h.events).toHaveLength(1);
});

test("debounce retains caret and flushes text before a business click exactly once", () => {
    const h = harness();
    h.editor.focus();
    h.setInputDebounce({ widget: "#1", milliseconds: 200 });
    input(h, "abc");
    input(h, "abXc");
    h.editor.setSelectionRange(3, 3);
    h.setText({ widget: "#1", text: "abc" });
    h.ack(0);
    expect(h.editor.value).toBe("abXc");
    expect(h.editor.selectionStart).toBe(3);
    expect(h.events).toHaveLength(0);
    h.sendEventToServer(h.button, "click", {});
    h.tick();
    expect(h.events.map(e => e.type)).toEqual(["text input", "click"]);
    expect(h.events[0].data.text).toBe("abXc");
});

test("an old acknowledgement cannot revive a stale value after blur", () => {
    const h = harness();
    h.editor.focus();
    input(h, "a");
    input(h, "ab");
    h.ack(1, "a");
    h.editor.blur();
    expect(h.editor.value).toBe("ab");
    h.ack(2);
    expect(h.editor.value).toBe("ab");
});

test("debounce emits the latest edit and disposal cancels scheduled input", () => {
    const h = harness();
    h.editor.focus();
    h.setInputDebounce({ widget: "#1", milliseconds: 200 });
    input(h, "a");
    input(h, "abc");
    h.tick();
    expect(h.events.map(e => e.data.text)).toEqual(["abc"]);
    input(h, "later");
    h.disposeWidget({ widget: "#1" });
    h.tick();
    expect(h.events).toHaveLength(1);
});

test("local focus and selection happen synchronously and survive a rejected business action", () => {
    const h = harness();
    expect(
        h.setClientEventAction({
            widget: "#2",
            event: "click",
            clientAction: { action: "focus widget", target: "#1" }
        })
    ).toBe(true);
    h.sendEventToServer(h.button, "click", {});
    expect(dom.window.document.activeElement).toBe(h.editor);
    expect(h.events).toHaveLength(1);
    // A rejected event is acknowledged without application updates; local UI is not a transaction.
    h.ack(1);
    expect(dom.window.document.activeElement).toBe(h.editor);
    h.editor.value = "abcdef";
    h.setClientEventAction({
        widget: "#2",
        event: "click",
        clientAction: { action: "select text", target: "#1", start: 1, end: 4 }
    });
    h.sendEventToServer(h.button, "click", {});
    expect(h.editor.selectionStart).toBe(1);
    expect(h.editor.selectionEnd).toBe(4);
    h.button.disabled = true;
    h.editor.setSelectionRange(0, 0);
    h.sendEventToServer(h.button, "click", {});
    expect(h.editor.selectionEnd).toBe(0);
});

test("local registry rejects server mutation actions, code, bad ranges and unsafe URLs", () => {
    const h = harness();
    for (const action of [
        { action: "set text", widget: "#1", text: "forged" },
        { action: "reset" },
        { action: "__proto__" },
        { action: "focus widget", target: "#1", code: "alert(1)" },
        { action: "select text", target: "#1", start: 3, end: 1 },
        { action: "open page in new tab", href: "javascript:alert(1)" },
        { action: "go to page", href: "data:text/html,test" }
    ])
        expect(h.setClientEventAction({ widget: "#2", event: "click", clientAction: action })).toBe(
            false
        );
    expect(h.validLocalAction({ action: "open page in new tab", href: "/safe" })).toBe(true);
});

test.each(["input field", "suggestion field"])(
    "detaching composition in %s cancels the draft without disabling input",
    type => {
        const h = harness(type);
        h.editor.focus();
        h.setKeyboardShortcuts({ widget: "#1", shortcuts: ["DOWN:Enter:0000:1"] });
        composition(h, "compositionstart");
        input(h, "未確定", true);
        h.pauseWidgetSubtree(h.editor);
        h.editor.remove();
        dom.window.document.body.append(h.editor);
        h.resumeWidgetSubtree(h.editor);
        expect(h.editor.value).toBe("");
        expect(h.events).toHaveLength(0);
        input(h, "after");
        h.editor.dispatchEvent(
            new dom.window.KeyboardEvent("keydown", { key: "Enter", bubbles: true })
        );
        expect(h.events.map(e => e.type)).toEqual(["text input", "keyboard"]);
    }
);
