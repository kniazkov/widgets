import { expect, test } from "@playwright/test";

test("AllWidgets displays arbitrary QR payloads as text", async ({ page }) => {
    await page.addInitScript(() => {
        Object.defineProperty(navigator, "mediaDevices", {
            value: { getUserMedia: async () => ({ getTracks: () => [{ stop() {} }] }) }
        });
        Object.defineProperty(HTMLMediaElement.prototype, "srcObject", {
            get() {
                return this._stream;
            },
            set(value) {
                this._stream = value;
            }
        });
        HTMLMediaElement.prototype.play = () => Promise.resolve();
        Object.defineProperty(HTMLMediaElement.prototype, "readyState", { get: () => 2 });
        Object.defineProperty(HTMLVideoElement.prototype, "videoWidth", { get: () => 320 });
        Object.defineProperty(HTMLVideoElement.prototype, "videoHeight", { get: () => 240 });
        const draw = CanvasRenderingContext2D.prototype.drawImage;
        CanvasRenderingContext2D.prototype.drawImage = function (source, ...args) {
            if (!(source instanceof HTMLVideoElement)) return draw.call(this, source, ...args);
        };
    });
    await page.goto("/all-widgets");
    await page.evaluate(() => {
        window.jsQR = () => (window.qrText ? { data: window.qrText } : null);
    });
    await page.getByRole("button", { name: "Enable camera", exact: true }).click();
    for (const text of [
        "https://food.example/buckwheat",
        "WIFI:T:WPA;S:Guest;P:sample;;",
        "<img src=x onerror=alert(1)> plain text"
    ]) {
        await page.evaluate(value => {
            window.qrText = value;
        }, text);
        await expect(page.getByText(text, { exact: true })).toBeVisible();
        await expect(page).toHaveURL(/\/all-widgets$/);
        await page.evaluate(() => {
            window.qrText = null;
        });
        await page.waitForTimeout(1800);
    }
});
