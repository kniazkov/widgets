import { readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it, vi } from "vitest";

const source = readScriptGroup("shared");

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
    it.each([
        [500, {}, null],
        [502, {}, null],
        [503, {}, null],
        [504, {}, null],
        [500, { "X-Widgets-Protocol-Error": "application" }, '{"clientError":true}'],
        [401, {}, '{"accessDenied":true}'],
        [413, {}, '{"clientError":true}'],
        [413, {}, '{"result":false,"uploadError":"requestTooLarge"}', "upload chunk"],
        [426, {}, '{"protocolMismatch":true}'],
        [200, { "X-Widgets-Protocol-Version": "2" }, '{"protocolMismatch":true}'],
        [200, { "X-Widgets-Protocol-Version": "1" }, "private body"],
        [200, {}, "private body"]
    ])(
        "classifies HTTP %i and protocol headers without leaking error bodies",
        (status, headers, expected, action = "synchronize") => {
            let request;
            class MockXmlHttpRequest {
                constructor() {
                    request = this;
                }
                open() {}
                send() {}
                getResponseHeader(name) {
                    return headers[name] ?? null;
                }
            }
            dom = new JSDOM("<!doctype html>", {
                runScripts: "outside-only",
                url: "http://localhost/"
            });
            dom.window.XMLHttpRequest = MockXmlHttpRequest;
            vi.spyOn(dom.window.console, "error").mockImplementation(() => {});
            dom.window.eval(source);
            const callback = vi.fn();
            dom.window.sendRequest({ action }, callback);
            request.readyState = 4;
            request.status = status;
            request.responseText = "private body";
            request.onreadystatechange();
            request.onerror();
            expect(callback).toHaveBeenCalledExactlyOnceWith(expected);
        }
    );

    it("caps exponential backoff and jitters attempts independently", () => {
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.eval(source);
        dom.window.Math.random = () => 0;
        expect(dom.window.transportRetryDelay(1)).toBe(375);
        expect(dom.window.transportRetryDelay(2)).toBe(750);
        dom.window.Math.random = () => 0.99;
        expect(dom.window.transportRetryDelay(1)).toBeGreaterThan(600);
        expect(dom.window.transportRetryDelay(1000000)).toBe(15000);
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

    it("sends every action as POST and keeps credentials in headers only", () => {
        const requests = [];
        class MockXmlHttpRequest {
            constructor() {
                this.headers = {};
                requests.push(this);
            }
            open(method, url) {
                this.method = method;
                this.url = url;
            }
            setRequestHeader(name, value) {
                this.headers[name] = value;
            }
            send(body) {
                this.body = body;
            }
        }
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        dom.window.eval(source);
        dom.window.configureProtocolSecurity("document-csrf-secret", "document-session-id");
        for (const action of [
            "new instance",
            "synchronize",
            "kill",
            "upload chunk",
            "report error"
        ]) {
            dom.window.sendRequest({ action, client: "#1" }, null, undefined, undefined, {
                token: "page-secret",
                epoch: "server-epoch"
            });
        }
        for (const request of requests) {
            expect(request.method).toBe("POST");
            expect(request.url).toBe("http://localhost");
            expect(request.headers).toEqual({
                "X-Widgets-CSRF": "document-csrf-secret",
                "X-Widgets-Session": "document-session-id",
                "X-Widgets-Channel": "page-secret",
                "X-Widgets-Epoch": "server-epoch"
            });
            expect([...request.body.keys()]).toEqual(["action", "client"]);
        }
    });

    it.each([
        ['{"sessionExpired":true}', { sessionExpired: true }],
        ['{"accessDenied":true}', { accessDenied: true }],
        ["private proxy error", { accessDenied: true }]
    ])("turns HTTP 403 into a controlled protocol denial: %s", (body, expected) => {
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
        vi.spyOn(dom.window.console, "error").mockImplementation(() => {});
        dom.window.eval(source);
        let result;
        dom.window.sendRequest({ action: "synchronize" }, value => {
            result = JSON.parse(value);
        });
        request.readyState = 4;
        request.status = 403;
        request.responseText = body;
        request.onreadystatechange();
        expect(result).toEqual(expected);
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
