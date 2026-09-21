import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it, vi } from "vitest";

const source = fs.readFileSync(new URL("../../main/html/scripts/lib.js", import.meta.url), "utf8");

let dom;

afterEach(() => {
    dom?.window.close();
});

describe("escapeHtml", () => {
    it("escapes every HTML-sensitive character", () => {
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.eval(source);

        expect(dom.window.escapeHtml(`<script data-x="a&b">'x'</script>`)).toBe(
            "&lt;script data-x=&quot;a&amp;b&quot;&gt;&#039;x&#039;&lt;/script&gt;"
        );
    });
});

describe("sendRequest", () => {
    it("preserves HTTP 500 as a fatal protocol result without exposing the response body", () => {
        let request;
        class MockXmlHttpRequest {
            constructor() {
                request = this;
            }
            open() {}
            send() {}
            getResponseHeader() {
                return null;
            }
        }
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        dom.window.eval(source);
        let result;
        dom.window.sendRequest({ action: "synchronize" }, data => {
            result = JSON.parse(data);
        });
        request.readyState = 4;
        request.status = 500;
        request.responseText = "private server stack";
        request.onreadystatechange();
        expect(result).toEqual({ result: false, clientError: true, httpStatus: 500 });
    });

    it("logs the rejected request ID without logging the response body", () => {
        let request;
        class MockXmlHttpRequest {
            constructor() {
                request = this;
            }
            open() {}
            send() {}
            getResponseHeader(name) {
                expect(name).toBe("X-Widgets-Request-Id");
                return "diagnostic-id";
            }
        }
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        const log = vi.spyOn(dom.window.console, "error").mockImplementation(() => {});
        dom.window.eval(source);
        const callback = vi.fn();
        dom.window.sendRequest({}, callback, "post");
        request.readyState = 4;
        request.status = 404;
        request.responseText = "private response";
        request.onreadystatechange();
        expect(log).toHaveBeenCalledExactlyOnceWith(
            "Widgets HTTP failure",
            404,
            "requestId=diagnostic-id"
        );
        expect(callback).toHaveBeenCalledExactlyOnceWith(null);
    });

    it("does not abort an in-flight request when another request starts", () => {
        const requests = [];

        class MockXmlHttpRequest {
            constructor() {
                this.readyState = 0;
                this.aborted = false;
                requests.push(this);
            }

            open() {
                if (this.readyState > 0 && this.readyState < 4) {
                    this.aborted = true;
                }
                this.readyState = 1;
            }

            send() {}
        }

        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        dom.window.eval(source);

        dom.window.sendRequest({ action: "first" });
        dom.window.sendRequest({ action: "second" });

        expect(requests).toHaveLength(2);
        expect(requests.every(request => !request.aborted)).toBe(true);
    });
});
