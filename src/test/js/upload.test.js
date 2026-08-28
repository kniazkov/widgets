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
    dom = new JSDOM("<!doctype html>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${optionsSource}\n${librarySource}\n
        configureUploadProtocol(4 * 1024, 128 * 1024 * 1024);
        window.__uploadEvents = [];
        window.__processedUpdates = [];
        let clientId = "#1";
        let lastProcessedUpdateId = 0;
        function createEvent(widget, type, data) {
            window.__uploadEvents.push({ widget, type, data });
        }
        function processUpdates(updates) {
            if (updates && updates.length) {
                window.__processedUpdates.push(...updates);
                lastProcessedUpdateId = Number(updates[updates.length - 1].id.slice(1));
            }
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
        ${widgetsSource}`);

    const requests = [];
    dom.window.sendRequest = (query, callback, method, files) => {
        requests.push({ query, callback, method, files });
    };
    return {
        requests,
        events: dom.window.__uploadEvents,
        processedUpdates: dom.window.__processedUpdates,
        widget: { _id: "#9" }
    };
}

async function waitForRequests(harness, count) {
    for (let attempt = 0; attempt < 20 && harness.requests.length < count; attempt++) {
        await new Promise(resolve => dom.window.setTimeout(resolve, 0));
    }
    expect(harness.requests.length).toBeGreaterThanOrEqual(count);
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
    it("registers every file immediately and round-robins five active files", async () => {
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
        await waitForRequests(harness, 1);
        expect(harness.requests).toHaveLength(1);
        expect(harness.requests[0].query.lastUpdate).toBe("#0");

        const sequence = [];
        for (let index = 0; index < 10; index++) {
            await waitForRequests(harness, index + 1);
            const request = harness.requests[index];
            sequence.push(request.query.fileId);
            expect(request.method).toBe("post");
            expect(request.files).toBeUndefined();
            expect(request.query.chunk).toMatch(/^[0-9a-f]+$/);
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
                await waitForRequests(harness, 2);
                expect(harness.requests[1].query.lastUpdate).toBe("#7");
            }
        }

        expect(sequence).toEqual([1, 2, 3, 4, 5, 1, 2, 3, 4, 5]);
        await waitForRequests(harness, 11);
        expect(harness.requests[10].query.fileId).toBe(6);
    });

    it("retries an unacknowledged chunk after giving other files a turn", async () => {
        const harness = createHarness();
        dom.window.loadFiles(harness.widget, [file("first.bin", 3), file("second.bin", 3)]);

        await waitForRequests(harness, 1);
        const firstAttempt = harness.requests[0];
        expect(firstAttempt.query.fileId).toBe(1);
        expect(firstAttempt.query.chunkIndex).toBe(0);
        firstAttempt.callback(null);

        await new Promise(resolve => dom.window.setTimeout(resolve, 300));

        await waitForRequests(harness, 2);
        const secondFile = harness.requests[1];
        expect(secondFile.query.fileId).toBe(2);
        secondFile.callback(JSON.stringify({ result: true, nextChunk: 1, complete: true }));

        await waitForRequests(harness, 3);
        const retry = harness.requests[2];
        expect(retry.query.fileId).toBe(1);
        expect(retry.query.chunkIndex).toBe(0);
        expect(retry.query.chunk).toBe(firstAttempt.query.chunk);
        expect(retry.query.chunk).toHaveLength(6);
    });
});
