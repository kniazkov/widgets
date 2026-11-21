import { JSDOM } from "jsdom";
import { afterEach, expect, it, vi } from "vitest";
import fs from "node:fs";
import { readScript } from "./helpers/scripts.js";
let dom;
afterEach(() => dom?.window.close());
const prefix = "https://shop.example/product?id=";
const code = prefix + "01234567-89ab-cdef-0123-456789abcdef";
function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        pretendToBeVisual: true,
        url: "https://shop.example"
    });
    const w = dom.window;
    w.eval(readScript("shared/jsqr.js"));
    w.send = vi.fn();
    w.eval(
        `const sendEventToServer = (...args) => window.send(...args); ${readScript("widgets/qr-scanner.js")}; window.api={QrPresentationGate,qrProductCode,createQrScanner,disposeQrScanners,decodeQrFrame};`
    );
    return w;
}
it("suppresses repeated frames and short gaps after acknowledgement", () => {
    const w = harness(),
        gate = new w.api.QrPresentationGate();
    expect(gate.frame(code, 0)).toBe(true);
    expect(gate.frame(code, 200)).toBe(false);
    expect(gate.frame(null, 2000)).toBe(false);
    gate.resume();
    expect(gate.frame(code, 3000)).toBe(false);
    gate.frame(null, 3200);
    gate.frame(null, 3500);
    expect(gate.frame(code, 3600)).toBe(false);
    gate.frame(null, 4000);
    gate.frame(null, 5600);
    expect(gate.frame(code, 5700)).toBe(true);
});
it("rejects unrelated or malformed URLs in filtered mode", () => {
    const w = harness();
    expect(w.api.qrProductCode(code, prefix)).toBe(code);
    for (const value of [
        "javascript:alert(1)",
        code + "&extra=1",
        code + "#x",
        code.replace("shop.example", "evil.example"),
        "/product?id=" + code.slice(prefix.length),
        prefix + "1-2-3-4-5"
    ])
        expect(w.api.qrProductCode(value, prefix)).toBeNull();
    expect(w.send).not.toHaveBeenCalled();
});
it("decodes a ZXing matrix both black-on-white and white-on-brown", () => {
    const w = harness(),
        matrix = JSON.parse(fs.readFileSync(new URL("./qr-matrix.json", import.meta.url), "utf8"));
    const size = matrix.length * 6;
    for (const inverse of [false, true]) {
        const pixels = new Uint8ClampedArray(size * size * 4);
        for (let y = 0; y < size; y++)
            for (let x = 0; x < size; x++) {
                const dark = matrix[Math.floor(y / 6)][Math.floor(x / 6)],
                    offset = (y * size + x) * 4;
                const color = inverse
                    ? dark
                        ? [255, 255, 255]
                        : [93, 64, 55]
                    : dark
                      ? [0, 0, 0]
                      : [255, 255, 255];
                pixels.set([...color, 255], offset);
            }
        expect(w.api.decodeQrFrame(pixels, size, size).data).toBe(code);
    }
});
function cameraHarness(getUserMedia) {
    const w = harness();
    Object.defineProperty(w, "isSecureContext", { value: true, configurable: true });
    Object.defineProperty(w.navigator, "mediaDevices", { value: { getUserMedia } });
    w.HTMLCanvasElement.prototype.getContext = () => ({
        drawImage() {},
        getImageData: () => ({ data: new Uint8ClampedArray(4), width: 1, height: 1 })
    });
    w.HTMLMediaElement.prototype.play = () => Promise.resolve();
    const widget = w.api.createQrScanner();
    w.document.body.append(widget);
    widget.getClientRects = () => [{}];
    expect(() => widget.setText("")).not.toThrow();
    widget.setText(JSON.stringify({ prefix, ack: 0, paused: false }));
    return { w, widget };
}
const settle = () => new Promise(resolve => setTimeout(resolve, 0));
function statuses(w) {
    return w.send.mock.calls.map(call => call[2].text).filter(text => text.startsWith("status\n"));
}
it("renders video only and never requests the camera while disabled", async () => {
    const get = vi.fn();
    const { widget } = cameraHarness(get);
    await settle();
    expect(widget.disabled).toBe(true);
    expect([...widget.children].map(child => child.tagName)).toEqual(["VIDEO"]);
    expect(widget.textContent).toBe("");
    expect(get).not.toHaveBeenCalled();
});
it("releases the camera on disable and starts a fresh stream on enable", async () => {
    const stop = vi.fn(),
        get = vi.fn(async () => ({ getTracks: () => [{ stop }] }));
    const { widget, w } = cameraHarness(get);
    widget.disabled = false;
    await settle();
    expect(get).toHaveBeenCalledTimes(1);
    expect(statuses(w)).toEqual(["status\nOPENING", "status\nREADY"]);
    widget.setText(JSON.stringify({ prefix, ack: 0, paused: true }));
    expect(stop).not.toHaveBeenCalled();
    expect(statuses(w).at(-1)).toBe("status\nWAITING");
    widget.disabled = true;
    expect(stop).toHaveBeenCalledTimes(1);
    expect(widget.querySelector("video").srcObject).toBeNull();
    widget.setText(JSON.stringify({ prefix, ack: 0, paused: false }));
    await settle();
    expect(get).toHaveBeenCalledTimes(1);
    widget.disabled = false;
    await settle();
    expect(get).toHaveBeenCalledTimes(2);
    w.api.disposeQrScanners();
    expect(stop).toHaveBeenCalledTimes(2);
});
it("releases the camera when removed and during page disposal", async () => {
    const stop = vi.fn();
    const { w, widget } = cameraHarness(async () => ({ getTracks: () => [{ stop }] }));
    widget.disabled = false;
    await settle();
    widget.remove();
    await settle();
    expect(stop).toHaveBeenCalledTimes(1);
    w.api.disposeQrScanners();
    expect(stop).toHaveBeenCalledTimes(1);
});
it("closes permission grants received after disable without replacing the new stream", async () => {
    const grants = [],
        oldStop = vi.fn(),
        newStop = vi.fn();
    const { widget } = cameraHarness(() => new Promise(resolve => grants.push(resolve)));
    widget.disabled = false;
    await settle();
    widget.disabled = true;
    widget.disabled = false;
    await settle();
    const current = { getTracks: () => [{ stop: newStop }] };
    grants[1](current);
    await settle();
    grants[0]({ getTracks: () => [{ stop: oldStop }] });
    await settle();
    expect(oldStop).toHaveBeenCalledTimes(1);
    expect(newStop).not.toHaveBeenCalled();
    expect(widget.querySelector("video").srcObject).toBe(current);
    widget.disabled = true;
    expect(newStop).toHaveBeenCalledTimes(1);
});
it("closes late permission grants after removal or hiding", async () => {
    for (const action of [
        widget => widget.remove(),
        widget => {
            widget.style.display = "none";
            widget.getClientRects = () => [];
        }
    ]) {
        let grant;
        const stop = vi.fn();
        const { widget, w } = cameraHarness(
            () =>
                new Promise(resolve => {
                    grant = resolve;
                })
        );
        widget.disabled = false;
        await settle();
        action(widget);
        await settle();
        grant({ getTracks: () => [{ stop }] });
        await settle();
        expect(stop).toHaveBeenCalledTimes(1);
        expect(statuses(w)).not.toContain("status\nREADY");
        w.api.disposeQrScanners();
        w.close();
    }
});
it("reports permission and insecure-context failures without rendering messages", async () => {
    const get = vi.fn(async () => {
        const error = new Error();
        error.name = "NotAllowedError";
        throw error;
    });
    const { w, widget } = cameraHarness(get);
    widget.disabled = false;
    await settle();
    expect(statuses(w).at(-1)).toBe("status\nDENIED");
    expect(widget.textContent).toBe("");
    // Model acknowledgements and DOM changes must not trigger an automatic retry.
    widget.setText(JSON.stringify({ prefix, ack: 0, paused: false }));
    w.document.body.append(w.document.createElement("p"));
    await settle();
    expect(get).toHaveBeenCalledTimes(1);
    widget.disabled = true;
    Object.defineProperty(w, "isSecureContext", { value: false });
    widget.disabled = false;
    await settle();
    expect(statuses(w).at(-1)).toBe("status\nHTTPS_REQUIRED");
    expect(get).toHaveBeenCalledTimes(1);
});
it("does not resume after page hiding until explicitly enabled again", async () => {
    const stop = vi.fn(),
        get = vi.fn(async () => ({ getTracks: () => [{ stop }] }));
    const { w, widget } = cameraHarness(get);
    widget.disabled = false;
    await settle();
    w.dispatchEvent(new w.Event("pagehide"));
    expect(stop).toHaveBeenCalledTimes(1);
    expect(statuses(w).at(-1)).toBe("status\nCLOSED");
    w.document.body.append(w.document.createElement("p"));
    await settle();
    expect(get).toHaveBeenCalledTimes(1);
    widget.disabled = true;
    widget.disabled = false;
    await settle();
    expect(get).toHaveBeenCalledTimes(2);
    widget.disabled = true;
});
it("ignores a stale playback failure after a replacement stream has started", async () => {
    const stop = vi.fn();
    const { w, widget } = cameraHarness(async () => ({ getTracks: () => [{ stop }] }));
    let rejectPlay;
    w.HTMLMediaElement.prototype.play = () =>
        new Promise((resolve, reject) => {
            rejectPlay = reject;
        });
    widget.disabled = false;
    await settle();
    widget.disabled = true;
    w.HTMLMediaElement.prototype.play = () => Promise.resolve();
    widget.disabled = false;
    await settle();
    rejectPlay(new Error("stale playback"));
    await settle();
    expect(stop).toHaveBeenCalledTimes(1);
    expect(statuses(w).at(-1)).toBe("status\nREADY");
    widget.disabled = true;
});

it("accepts arbitrary QR text in general mode without executing it", () => {
    const w = harness();
    for (const value of [
        "https://food.example/buckwheat",
        "WIFI:T:WPA;S:Guest;P:test;;",
        "plain text",
        "Привет 🌍",
        "<script>alert(1)</script>",
        "7".repeat(7089)
    ]) {
        expect(w.api.qrProductCode(value, "")).toBe(value);
    }
    expect(w.api.qrProductCode("", "")).toBeNull();
    expect(w.send).not.toHaveBeenCalled();
});

it("resumes the acknowledged sequence and retries a code interrupted by disable", async () => {
    const stop = vi.fn();
    const { w, widget } = cameraHarness(async () => ({ getTracks: () => [{ stop }] }));
    Object.defineProperty(w.HTMLMediaElement.prototype, "readyState", { get: () => 2 });
    Object.defineProperty(w.HTMLVideoElement.prototype, "videoWidth", { get: () => 1 });
    Object.defineProperty(w.HTMLVideoElement.prototype, "videoHeight", { get: () => 1 });
    w.jsQR = () => ({ data: code });
    widget.setText(JSON.stringify({ prefix, ack: 5, paused: false }));
    widget.disabled = false;
    await settle();
    const codes = () =>
        w.send.mock.calls.map(call => call[2].text).filter(text => !text.startsWith("status\n"));
    expect(codes()).toEqual(["6\n" + code]);
    widget.disabled = true;
    widget.disabled = false;
    await settle();
    expect(codes()).toEqual(["6\n" + code, "6\n" + code]);
    widget.disabled = true;
});
