import assert from "node:assert/strict";

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

export function checkImageInventory(images, diskEntries, documentation) {
    const files = Object.values(images).map(image => image.file);
    for (const file of files) {
        assert(/^[a-z0-9-]+\.png$/.test(file), `Invalid image filename: ${file}`);
    }
    assert.equal(new Set(files).size, files.length, "An image file must have one manifest entry");
    assert.deepEqual(
        files.sort(),
        diskEntries.filter(file => /\.png$/i.test(file)).sort(),
        "The manifest and PNG files must match"
    );

    const entries = [...documentation.matchAll(/!\[([^\]\n]*)\]\(images\/widgets\/([^\s)]+)\)/g)];
    for (const [, alt, file] of entries) {
        assert(alt.trim(), `Missing image alt text: ${file}`);
    }
    assert.deepEqual(
        entries.map(([, , file]) => file).sort(),
        files,
        "Document every gallery PNG exactly once using a Markdown image and alt text"
    );
}

export function checkGalleryImage(id, image, bytes, captures) {
    assert(
        bytes.length >= 33 &&
            bytes.subarray(0, 8).equals(PNG_SIGNATURE) &&
            bytes.readUInt32BE(8) === 13 &&
            bytes.subarray(12, 16).toString() === "IHDR",
        `Not a PNG with an IHDR: ${id}`
    );
    assert(
        image.title?.trim() && image.states?.trim() && image.sourceScreenshot?.trim(),
        `Missing capture description: ${id}`
    );
    assert(
        Array.isArray(image.crop) &&
            image.crop.length === 4 &&
            image.crop.every(value => Number.isSafeInteger(value) && value >= 0) &&
            image.crop[2] > 0 &&
            image.crop[3] > 0,
        `Invalid crop: ${id}`
    );
    assert.deepEqual(
        [bytes.readUInt32BE(16), bytes.readUInt32BE(20)],
        image.crop.slice(2),
        `PNG dimensions differ from unscaled crop: ${id}`
    );
    if (image.captureId !== undefined) {
        const capture = Object.hasOwn(captures, image.captureId) && captures[image.captureId];
        assert(
            capture?.date?.trim() &&
                /^[0-9a-f]{40}$/.test(capture.commit) &&
                /^[0-9a-f]{64}$/.test(capture.allWidgetsSourceSha256) &&
                capture.page?.trim() &&
                capture.browser?.trim() &&
                capture.viewport?.trim() &&
                capture.method?.trim(),
            `Missing capture date, revision, page, browser, viewport or method: ${id}`
        );
        assert(
            Object.hasOwn(capture.screenshots ?? {}, image.sourceScreenshot),
            `Missing source screenshot: ${id}`
        );
        const bounds = capture.screenshots[image.sourceScreenshot];
        assert(
            Array.isArray(bounds) &&
                bounds.length === 2 &&
                bounds.every(value => Number.isSafeInteger(value) && value > 0) &&
                image.crop[0] + image.crop[2] <= bounds[0] &&
                image.crop[1] + image.crop[3] <= bounds[1],
            `Crop exceeds source screenshot: ${id}`
        );
    }
}
