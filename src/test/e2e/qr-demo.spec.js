import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
        Object.defineProperty(navigator, "mediaDevices", {
            value: {
                getUserMedia: async () => {
                    window.cameraRequests = (window.cameraRequests || 0) + 1;
                    if (window.denyCamera) throw new DOMException("Denied", "NotAllowedError");
                    return {
                        getTracks: () => [
                            {
                                stop() {
                                    window.cameraStops = (window.cameraStops || 0) + 1;
                                }
                            }
                        ]
                    };
                }
            }
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
});

test("external controls toggle the video-only scanner and display arbitrary QR text", async ({
    page
}) => {
    const scanner = page.locator("video").locator("..");
    await expect(scanner.locator("button, p")).toHaveCount(0);
    expect(await page.evaluate(() => window.cameraRequests || 0)).toBe(0);
    await page.getByRole("button", { name: "Enable camera", exact: true }).click();
    await expect(
        page.getByText("Show a QR. Remove it for two seconds before scanning it again.", {
            exact: true
        })
    ).toBeVisible();
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
    await page.getByRole("button", { name: "Disable camera", exact: true }).click();
    await expect(page.getByText("Camera is disabled.", { exact: true })).toBeVisible();
    await expect.poll(() => page.evaluate(() => window.cameraStops)).toBe(1);
    expect(await page.locator("video").evaluate(video => video.srcObject)).toBeNull();
    await page.getByRole("button", { name: "Enable camera", exact: true }).click();
    await expect.poll(() => page.evaluate(() => window.cameraRequests)).toBe(2);
    await page.evaluate(() => {
        window.qrText = "after restart";
    });
    await expect(page.getByText("after restart", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Disable camera", exact: true }).click();
    await expect.poll(() => page.evaluate(() => window.cameraStops)).toBe(2);
});

test("external status displays denial and allows another enable attempt", async ({ page }) => {
    await page.evaluate(() => {
        window.denyCamera = true;
    });
    await page.getByRole("button", { name: "Enable camera", exact: true }).click();
    await expect(page.getByText("Camera permission was denied.", { exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Enable camera", exact: true })).toBeVisible();
    await page.evaluate(() => {
        window.denyCamera = false;
    });
    await page.getByRole("button", { name: "Enable camera", exact: true }).click();
    await expect(
        page.getByText("Show a QR. Remove it for two seconds before scanning it again.", {
            exact: true
        })
    ).toBeVisible();
    expect(await page.evaluate(() => window.cameraRequests)).toBe(2);
    await page.getByRole("button", { name: "Disable camera", exact: true }).click();
});
