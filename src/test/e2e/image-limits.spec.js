import { expect, test } from "@playwright/test";

test("SVG and PNG preserve their ratio within two limits and a narrow parent", async ({ page }) => {
    await page.goto("/");
    const sizes = await page.evaluate(async () => {
        const svg =
            "data:image/svg+xml," +
            encodeURIComponent(
                '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="200" viewBox="0 0 800 200"><rect width="800" height="200" fill="blue"/></svg>'
            );
        const canvas = document.createElement("canvas");
        canvas.width = 800;
        canvas.height = 200;
        const results = [];
        for (const [index, source] of [svg, canvas.toDataURL()].entries()) {
            const id = "limit-image-" + index;
            createWidget({ type: "image", widget: id });
            const image = widgets[id];
            const parent = document.createElement("div");
            parent.style.width = "480px";
            document.body.appendChild(parent);
            parent.appendChild(image);
            image.src = source;
            await image.decode();
            actionHandlers["set max width"]({ widget: id, "max width": "100%" });
            actionHandlers["set max height"]({ widget: id, "max height": "80px" });
            const measure = () => ({
                width: image.getBoundingClientRect().width,
                height: image.getBoundingClientRect().height
            });
            const heightLimited = measure();
            parent.style.width = "160px";
            const widthLimited = measure();
            actionHandlers["set max height"]({ widget: id, "max height": "" });
            actionHandlers["set max width"]({ widget: id, "max width": "" });
            results.push({ heightLimited, widthLimited, reset: measure() });
            parent.remove();
        }
        return results;
    });
    for (const result of sizes) {
        expect(result.heightLimited).toEqual({ width: 320, height: 80 });
        expect(result.widthLimited).toEqual({ width: 160, height: 40 });
        expect(result.reset).toEqual({ width: 800, height: 200 });
    }
});
