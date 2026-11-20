import { describe, expect, it } from "vitest";
import {
    checkGalleryImage,
    checkImageInventory
} from "../../../scripts/widget-gallery-evidence.mjs";

const image = {
    file: "example.png",
    title: "Current gallery example",
    states: "Open popup",
    sourceScreenshot: "popup.jpg",
    captureId: "test-capture",
    crop: [2, 3, 20, 10]
};
const captures = {
    "test-capture": {
        date: "2026-11-14",
        commit: "1234567890abcdef1234567890abcdef12345678",
        page: "AllWidgets /",
        allWidgetsSourceSha256: "a".repeat(64),
        browser: "Cloud Chromium 156",
        viewport: "500 × 740",
        method: "Native screenshot without scaling",
        screenshots: { "popup.jpg": [24, 14] }
    }
};

function pngHeader() {
    const bytes = Buffer.alloc(33);
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(bytes);
    bytes.writeUInt32BE(13, 8);
    bytes.write("IHDR", 12);
    bytes.writeUInt32BE(20, 16);
    bytes.writeUInt32BE(10, 20);
    return bytes;
}

describe("widget gallery evidence", () => {
    it("accepts an unscaled PNG crop inside its original frame", () => {
        expect(() => checkGalleryImage("example", image, pngHeader(), captures)).not.toThrow();
    });

    it("rejects incorrect dimensions, unsafe coordinates, and out-of-frame crops", () => {
        for (const [crop, message] of [
            [[2, 3, 21, 10], "dimensions differ"],
            [[-1, 3, 20, 10], "Invalid crop"],
            [[5, 3, 20, 10], "exceeds source"]
        ]) {
            expect(() =>
                checkGalleryImage("example", { ...image, crop }, pngHeader(), captures)
            ).toThrow(message);
        }
    });

    it("rejects incomplete PNG and missing screenshot provenance", () => {
        expect(() => checkGalleryImage("example", image, Buffer.from(" PNG"), captures)).toThrow(
            "Not a PNG"
        );
        expect(() => checkGalleryImage("example", image, pngHeader(), {})).toThrow(
            "Missing capture date"
        );
        expect(() =>
            checkGalleryImage(
                "example",
                { ...image, sourceScreenshot: "other.jpg" },
                pngHeader(),
                captures
            )
        ).toThrow("Missing source screenshot");
    });

    it("rejects unlisted files and missing or duplicate image embeddings", () => {
        const images = { example: image };
        const docs = "![Popup in its open state](../../images/widgets/example.png)";
        expect(() =>
            checkImageInventory(images, ["example.png", "manifest.json"], docs)
        ).not.toThrow();
        expect(() => checkImageInventory(images, ["example.png", "unlisted.png"], docs)).toThrow(
            "manifest and PNG files must match"
        );
        expect(() =>
            checkImageInventory(images, ["example.png"], "![](../../images/widgets/example.png)")
        ).toThrow("Missing image alt");
        expect(() => checkImageInventory(images, ["example.png"], docs + "\n" + docs)).toThrow(
            "exactly once"
        );
    });
});
