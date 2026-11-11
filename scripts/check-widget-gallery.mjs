import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile, readdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const view = join(root, "src/main/java/com/kniazkov/widgets/view");
const imageDirectory = join(root, "docs/images/widgets");
const manifest = JSON.parse(await readFile(join(imageDirectory, "manifest.json"), "utf8"));
const documentation = await readFile(join(root, "docs/WIDGET_GALLERY.ru.md"), "utf8");
const classes = new Map();

// Inspect public top-level declarations, removing comments and nested generic parameters.
// Compilation remains the Java syntax check; this is a small documentation coverage gate.
for (const file of await readdir(view)) {
    if (!file.endsWith(".java")) continue;
    const source = (await readFile(join(view, file), "utf8")).replace(
        /\/\*[\s\S]*?\*\/|\/\/[^\n]*/g,
        ""
    );
    const declaration = /public\s+(?:(abstract|final)\s+)?class\s+(\w+)([^\{]*)\{/.exec(source);
    if (!declaration) continue;
    let header = declaration[3];
    while (/<[^<>]*>/.test(header)) header = header.replace(/<[^<>]*>/g, "");
    classes.set(declaration[2], {
        abstract: declaration[1] === "abstract",
        parent: /\bextends\s+(\w+)/.exec(header)?.[1]
    });
}

function isWidget(name, ancestors = new Set()) {
    if (name === "Widget") return true;
    if (!classes.has(name)) return false;
    assert(!ancestors.has(name), `Inheritance cycle: ${name}`);
    ancestors.add(name);
    return isWidget(classes.get(name).parent, ancestors);
}

const visible = [...classes.keys()].filter(name => isWidget(name) && !classes.get(name).abstract);
assert.deepEqual(
    Object.keys(manifest.widgets).sort(),
    visible.sort(),
    "Every public concrete Widget needs a screenshot entry; remove obsolete entries too."
);
for (const [name, images] of Object.entries(manifest.widgets)) {
    assert(images.length > 0, `No image for ${name}`);
    assert(documentation.includes(`\`${name}\``), `Missing documented class: ${name}`);
    for (const id of images) assert(manifest.images[id], `Unknown image ${id} for ${name}`);
}
for (const [id, image] of Object.entries(manifest.images)) {
    assert(/^[a-z0-9-]+\.png$/.test(image.file), `Invalid image filename: ${id}`);
    const bytes = await readFile(join(imageDirectory, image.file));
    assert.equal(bytes.subarray(1, 4).toString(), "PNG", `Not a PNG: ${id}`);
    assert.equal(
        createHash("sha256").update(bytes).digest("hex"),
        image.sha256,
        `Stale hash: ${id}`
    );
    assert(documentation.includes(`](images/widgets/${image.file})`), `Image not embedded: ${id}`);
    assert(image.states && image.sourceScreenshot, `Missing capture description: ${id}`);
    assert(
        image.crop.length === 4 && image.crop[2] > 0 && image.crop[3] > 0,
        `Invalid crop: ${id}`
    );
}
console.log(
    `Gallery coverage: ${visible.length} widget classes, ${Object.keys(manifest.images).length} images.`
);
