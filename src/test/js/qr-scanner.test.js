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
it("не повторяет кадры, короткое размытие и код после подтверждения", () => {
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
it("не принимает внешние, относительные, повреждённые и исполняемые ссылки", () => {
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
it("декодирует матрицу ZXing обычную и белую на коричневом", () => {
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
    w.HTMLCanvasElement.prototype.getContext = () => ({});
    w.HTMLMediaElement.prototype.play = () => Promise.resolve();
    const widget = w.api.createQrScanner();
    w.document.body.append(widget);
    widget.getClientRects = () => [{}];
    const messages = Object.fromEntries(
        [
            "start",
            "idle",
            "opening",
            "ready",
            "waiting",
            "https",
            "denied",
            "unavailable",
            "invalid",
            "closed"
        ].map(x => [x, x])
    );
    widget.setText(JSON.stringify({ prefix, messages, ack: 0, paused: false }));
    return { w, widget, button: widget.querySelector("button") };
}
it("освобождает камеру при удалении и завершении страницы", async () => {
    const stop = vi.fn(),
        stream = { getTracks: () => [{ stop }] };
    const { w, widget, button } = cameraHarness(async () => stream);
    button.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    widget.remove();
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(stop).toHaveBeenCalledTimes(1);
    w.api.disposeQrScanners();
    expect(stop).toHaveBeenCalledTimes(1);
});
it("закрывает поток, если разрешение пришло после закрытия окна", async () => {
    let grant;
    const stop = vi.fn();
    const { widget, button } = cameraHarness(
        () =>
            new Promise(resolve => {
                grant = resolve;
            })
    );
    button.click();
    widget.remove();
    await new Promise(resolve => setTimeout(resolve, 0));
    grant({ getTracks: () => [{ stop }] });
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(stop).toHaveBeenCalledTimes(1);
});
it("показывает отказ в разрешении и HTTP без запуска камеры", async () => {
    const get = vi.fn(async () => {
        const error = new Error();
        error.name = "NotAllowedError";
        throw error;
    });
    const { w, widget, button } = cameraHarness(get);
    button.click();
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(widget.textContent).toContain("denied");
    Object.defineProperty(w, "isSecureContext", { value: false });
    button.click();
    expect(widget.textContent).toContain("https");
    expect(get).toHaveBeenCalledTimes(1);
    w.api.disposeQrScanners();
});
