import { readScript, readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const optionsSource = readScript("options.js");
const librarySource = readScriptGroup("shared");
const widgetsSource = readScriptGroup("widgets");

let dom;

afterEach(() => {
    dom?.window.close();
});

function createHarness() {
    dom = new JSDOM("<!doctype html>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${optionsSource}\n${librarySource}\n
        configureUploadProtocol(4 * 1024, 128 * 1024 * 1024);
        window.__uploadEvents = [];
        window.__processedUpdates = [];
        let clientId = "#1";
        let clientDisposed = false;
        let lastAppliedUpdateSequence = 0;
        function createEvent(widget, type, data) {
            window.__uploadEvents.push({ widget, type, data });
        }
        function processUpdates({ updates }) {
            if (updates && updates.length) {
                window.__processedUpdates.push(...updates);
                lastAppliedUpdateSequence = Number(updates[updates.length - 1].id.slice(1));
            }
            return true;
        }
        function sendSynchronizeRequest(callback) {
            callback(true);
        }
        function recordRequestFailure() {
            window.__requestFailures = (window.__requestFailures || 0) + 1;
        }
        function recordRequestSuccess() {
            window.__requestSuccesses = (window.__requestSuccesses || 0) + 1;
        }
        ${widgetsSource}
        window.__dispose = () => { clientDisposed = true; disposeUploads(); };
        window.__files = () => [...pendingUploads, ...activeUploads];`);

    const requests = [];
    dom.window.sendClientRequest = (query, callback, method, files) => {
        requests.push({ query, callback, method, files });
    };
    return {
        requests,
        events: dom.window.__uploadEvents,
        processedUpdates: dom.window.__processedUpdates,
        widget: { _id: "#9" }
    };
}

function file(name, size) {
    const content = new Uint8Array(size);
    for (let index = 0; index < content.length; index++) {
        content[index] = index % 251;
    }
    return new dom.window.File([content], name, {
        type: "application/octet-stream"
    });
}

describe("binary upload scheduler", () => {
    it("registers every file immediately and round-robins five active files", () => {
        const harness = createHarness();
        dom.window.FileReader = class {
            constructor() {
                throw new Error("FileReader must not be used by binary uploads");
            }
        };
        const files = Array.from({ length: 7 }, (_, index) =>
            file(`file-${index + 1}.bin`, 4 * 1024 + 1)
        );

        dom.window.loadFiles(harness.widget, files);

        expect(harness.events).toHaveLength(7);
        expect(harness.events.every(event => event.type === "upload")).toBe(true);
        expect(harness.events.map(event => event.data.fileId)).toEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(harness.events.every(event => !("content" in event.data))).toBe(true);
        expect(harness.requests).toHaveLength(1);
        expect(harness.requests[0].query.lastUpdate).toBe("#0");

        const sequence = [];
        for (let index = 0; index < 10; index++) {
            const request = harness.requests[index];
            sequence.push(request.query.fileId);
            expect(request.method).toBe("post");
            expect(request.files[0].data).toBeInstanceOf(dom.window.Blob);
            request.callback(
                JSON.stringify({
                    result: true,
                    nextChunk: request.query.chunkIndex + 1,
                    complete: request.query.chunkIndex == 1,
                    updates: index == 0 ? [{ id: "#7" }] : []
                })
            );
            if (index == 0) {
                expect(harness.processedUpdates).toEqual([{ id: "#7" }]);
                expect(harness.requests[1].query.lastUpdate).toBe("#7");
            }
        }

        expect(sequence).toEqual([1, 2, 3, 4, 5, 1, 2, 3, 4, 5]);
        expect(harness.requests[10].query.fileId).toBe(6);
    });

    it("retries an unacknowledged chunk after giving other files a turn", async () => {
        const harness = createHarness();
        dom.window.loadFiles(harness.widget, [file("first.bin", 3), file("second.bin", 3)]);

        const firstAttempt = harness.requests[0];
        expect(firstAttempt.query.fileId).toBe(1);
        expect(firstAttempt.query.chunkIndex).toBe(0);
        firstAttempt.callback(null);

        await new Promise(resolve => dom.window.setTimeout(resolve, 300));

        const secondFile = harness.requests[1];
        expect(secondFile.query.fileId).toBe(2);
        secondFile.callback(JSON.stringify({ result: true, nextChunk: 1, complete: true }));

        const retry = harness.requests[2];
        expect(retry.query.fileId).toBe(1);
        expect(retry.query.chunkIndex).toBe(0);
        expect(retry.files[0].data.size).toBe(3);
    });
});

it("releases browser files and stops retries on disposal", async () => {
    const harness = createHarness();
    dom.window.loadFiles(harness.widget, [file("cancel.bin", 8192)]);
    const selected = dom.window.__files();
    harness.requests[0].callback(null);
    dom.window.__dispose();
    expect(selected.every(item => item.source === null)).toBe(true);
    expect(dom.window.__files()).toHaveLength(0);
    await new Promise(resolve => setTimeout(resolve, 350));
    expect(harness.requests).toHaveLength(1);
});

it("reports size errors without sending a file and clears feedback on the next selection", () => {
    const harness = createHarness();
    const notice = () => dom.window.document.querySelector(".widgets-upload-error");
    dom.window.loadFiles(harness.widget, [{ name: "large.bin", size: 128 * 1024 * 1024 + 1 }]);
    expect(notice().getAttribute("role")).toBe("status");
    expect(notice().dataset.uploadError).toBe("fileTooLarge");
    expect(harness.requests).toHaveLength(0);
    dom.window.loadFiles(harness.widget, [file("x".repeat(256), 1)]);
    expect(notice().dataset.uploadError).toBe("invalidFileName");
    expect(harness.events).toHaveLength(0);
    dom.window.loadFiles(harness.widget, [file("small.bin", 1)]);
    expect(notice()).toBeNull();
    expect(harness.requests).toHaveLength(1);
});

it("distinguishes a permanent chunk-size rejection from the next valid upload", () => {
    const harness = createHarness();
    dom.window.loadFiles(harness.widget, [file("bad.bin", 3), file("next.bin", 3)]);
    harness.requests[0].callback(
        JSON.stringify({ result: false, uploadError: "chunkTooLarge", limitBytes: 2 })
    );
    expect(dom.window.document.querySelector(".widgets-upload-error").textContent).toContain(
        "Maximum chunk size: 2 bytes."
    );
    expect(harness.requests).toHaveLength(2);
    expect(harness.requests[1].query.fileId).toBe(2);
    expect(harness.requests[1].query.chunkIndex).toBe(0);
});

it("does not retry a full processing queue and opens the next file", () => {
    const harness = createHarness();
    dom.window.loadFiles(harness.widget, [file("busy.bin", 3), file("remaining.bin", 3)]);
    harness.requests[0].callback(JSON.stringify({ result: false, uploadError: "processingBusy" }));
    expect(dom.window.document.querySelector(".widgets-upload-error").textContent).toContain(
        "File processing is busy"
    );
    expect(harness.requests).toHaveLength(2);
    expect(harness.requests[1].query.fileId).toBe(2);
});
