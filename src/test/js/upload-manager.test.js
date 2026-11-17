import { readScript, readScriptGroup } from "./helpers/scripts.js";
import { JSDOM } from "jsdom";
import { afterEach, expect, test } from "vitest";

let dom;
afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", { runScripts: "outside-only" });
    dom.window.eval(`${readScript("options.js")}\n${readScriptGroup("shared")}\n
        configureUploadProtocol(4, 100);
        const sent = [];
        function createEvent(widget, type, data) { sent.push({type, data}); }
        function sendEventToServer(widget, type, data) { sent.push({type, data}); }
        function sendSynchronizeRequest() {}
        ${readScriptGroup("widgets")}
        const manager = createUploadManager(); manager._id = "#m";
        const list = createUploadList(); list._id = "#l";
        widgets["#m"] = manager; widgets["#l"] = list;
        document.body.append(manager, list);
        setUploadManager({widget: "#m", snapshot: {list: "#l", maxFiles: 3, maxBytes: 10}});
        window.h = {manager, list, sent, files: () => [...pendingUploads, ...activeUploads],
            update: state => setUploadManager({widget: "#m", snapshot: {list: "#l", maxFiles: 3, maxBytes: 10, ...state}}),
            discard: () => discardWidgetUploads(manager), dispose: disposeUploads};`);
    return dom.window.h;
}
function file(name, size = 8) {
    return new dom.window.File([new Uint8Array(size)], name);
}

test("queues files, isolates errors and retries one logical row with the original attempt ID", () => {
    const h = harness();
    h.manager._managedLoad([file("a.txt"), file("b.txt")]);
    expect(h.files()).toHaveLength(2);
    h.update({ fileId: 1, state: "error", retry: true, message: "Processing failed" });
    expect(h.files()).toHaveLength(1);
    expect(h.list.textContent).toContain("b.txt — queued");
    h.update({ fileId: 1, state: "error", retry: true, message: "retry" });
    expect(h.sent.at(-1).data.retryOf).toBe(1);
    expect(h.list.querySelectorAll(".widgets-upload-row")).toHaveLength(2);
    expect(h.files()).toHaveLength(2);
    h.update({ fileId: 3, state: "done", retry: false, message: "" });
    expect(h.manager._managedFiles.get(3).source).toBeNull();
    expect(h.list.textContent).toContain("1/2");
});

test("cancel removes queued binary work and disposal releases retry sources", () => {
    const h = harness();
    h.manager._managedLoad([file("a.txt"), file("b.txt")]);
    h.update({ fileId: 1, state: "cancelled", retry: true, message: "cancel" });
    expect(h.files().map(f => f.id)).toEqual([2]);
    expect(h.manager._managedFiles.get(1).source).not.toBeNull();
    h.discard();
    expect(h.files()).toHaveLength(0);
    expect(h.manager._managedFiles.size).toBe(0);
});

test("local validation IDs stay unique after removal and count remains bounded", () => {
    const h = harness();
    h.manager._managedLoad([file("big1.txt", 11), file("big2.txt", 12)]);
    h.list.querySelector("button").click();
    h.manager._managedLoad([file("big3.txt", 13), file("a.txt"), file("extra.txt")]);
    expect(h.manager._managedFiles.size).toBe(3);
    expect(h.sent).toHaveLength(1);
    expect(h.list.textContent).toContain("File count limit reached");
    expect(h.list.textContent).toContain("big2.txt");
    expect(h.list.textContent).toContain("big3.txt");
    h.dispose();
    expect(h.manager._managedFiles.size).toBe(0);
});

test("late progress cannot overwrite terminal states and zero-byte completion reaches 100%", () => {
    const h = harness();
    h.manager._managedLoad([file("empty.txt", 0)]);
    const transfer = h.files()[0];
    h.update({ fileId: 1, state: "done", retry: false, message: "" });
    h.manager._managedProgress(transfer, "uploading");
    expect(h.list.textContent).toContain("1/1 · 100%");
    expect(h.list.querySelector(".widgets-upload-row").dataset.uploadState).toBe("done");
});

test("progress updates preserve the focused action node during a pointer click", () => {
    const h = harness();
    h.manager._managedLoad([file("a.txt")]);
    const button = h.list.querySelector("button");
    button.focus();
    const transfer = h.files()[0];
    transfer.nextChunk = 1;
    h.manager._managedProgress(transfer, "uploading");
    expect(h.list.querySelector("button")).toBe(button);
    expect(dom.window.document.activeElement).toBe(button);
    button.click();
    expect(h.sent.at(-1)).toEqual({
        type: "upload command",
        data: { action: "cancel", fileId: 1 }
    });
});

test("disabling the manager immediately disables list actions without awaiting progress", () => {
    const h = harness();
    h.manager._managedLoad([file("a.txt")]);
    const button = h.list.querySelector("button");
    h.manager.disabled = true;
    expect(button.disabled).toBe(true);
    expect(h.manager.hasAttribute("disabled")).toBe(true);
    button.click();
    expect(h.sent).toHaveLength(1);
    h.manager.disabled = false;
    expect(button.disabled).toBe(false);
    button.click();
    expect(h.sent).toHaveLength(2);
});
