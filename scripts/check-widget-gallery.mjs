import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile, readdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { checkGalleryImage, checkImageInventory } from "./widget-gallery-evidence.mjs";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const view = join(root, "src/main/java/com/kniazkov/widgets/view");
const imageDirectory = join(root, "docs/images/widgets");
const manifest = JSON.parse(await readFile(join(imageDirectory, "manifest.json"), "utf8"));
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

const visible = [...classes.keys()]
    .filter(name => isWidget(name) && !classes.get(name).abstract)
    .sort();
assert.deepEqual(
    Object.keys(manifest.widgets).sort(),
    visible,
    "Every public concrete Widget needs a screenshot entry; remove obsolete entries too."
);
for (const [name, images] of Object.entries(manifest.widgets)) {
    assert(images.length > 0, `No image for ${name}`);
    for (const id of images) assert(manifest.images[id], `Unknown image ${id} for ${name}`);
}
const diskImages = await readdir(imageDirectory);

async function checkCatalog(locale, indexName, otherLocale, otherIndex) {
    const directory = join(root, "docs", locale, "widgets");
    const index = await readFile(join(root, "docs", indexName), "utf8");
    assert(index.includes(`](${otherIndex})`), `Missing language switch in ${indexName}`);
    const chapters = new Map();
    for (const file of (await readdir(directory)).filter(name => name.endsWith(".md"))) {
        const source = await readFile(join(directory, file), "utf8");
        assert(index.includes(`](${locale}/widgets/${file})`), `Missing topic: ${locale}/${file}`);
        assert(
            source.includes(`](../../${otherLocale}/widgets/${file})`),
            `Missing translation: ${file}`
        );
        assert(source.includes(`](../../${indexName})`), `Missing catalog link: ${file}`);
        chapters.set(file, source);
    }
    const entries = new Map();
    for (const [file, source] of chapters) {
        for (const section of source.split(/^## /m).slice(1)) {
            const name = section.split("\n", 1)[0];
            if (!/^\w+$/.test(name)) continue;
            const ids = manifest.widgets[name];
            assert(ids && !entries.has(name), `Unknown or duplicate entry: ${locale}/${name}`);
            const code = /^```java\n([\s\S]+?)\n```$/m.exec(section);
            assert(
                code && section.slice(name.length, code.index).trim(),
                `Missing example or description: ${name}`
            );
            const shot = /\]\(([^)#]*)#shot-([a-z0-9-]+)\)/.exec(section);
            assert(shot && ids.includes(shot[2]), `Missing screenshot for ${name}`);
            const gallery = chapters.get(shot[1] || file) ?? "";
            const anchor = `<a id="shot-${shot[2]}"></a>`;
            const frame = gallery.split(anchor)[1]?.split('<a id="shot-')[0];
            assert(
                frame?.includes(`](../../images/widgets/${manifest.images[shot[2]].file})`),
                `Missing linked image for ${name}`
            );
            assert(
                section.includes(
                    `](../../../src/main/java/com/kniazkov/widgets/view/${name}.java)`
                ),
                `Missing Java source for ${name}`
            );
            assert(
                index.includes(`[${name}](${locale}/widgets/${file}#${name.toLowerCase()})`),
                `Missing index link: ${name}`
            );
            entries.set(name, { file, example: code[1], image: shot[2] });
        }
    }
    const indexed = [...index.matchAll(/\[(\w+)\]\((?:en|ru)\/widgets\/[\w-]+\.md#/g)].map(
        match => match[1]
    );
    assert.deepEqual(indexed, visible, `Alphabetical widget index: ${locale}`);
    assert.deepEqual([...entries.keys()].sort(), visible, `Missing widget entries: ${locale}`);
    checkImageInventory(manifest.images, diskImages, [...chapters.values()].join("\n"));
    return entries;
}

assert.deepEqual(
    await checkCatalog("en", "WIDGETS.md", "ru", "WIDGET_GALLERY.ru.md"),
    await checkCatalog("ru", "WIDGET_GALLERY.ru.md", "en", "WIDGETS.md"),
    "Keep the widget examples, screenshots and task grouping in sync across languages"
);
for (const [id, image] of Object.entries(manifest.images)) {
    const bytes = await readFile(join(imageDirectory, image.file));
    assert.equal(
        createHash("sha256").update(bytes).digest("hex"),
        image.sha256,
        `Stale hash: ${id}`
    );
    checkGalleryImage(id, image, bytes, manifest.captures ?? {});
}
console.log(
    `Gallery coverage: ${visible.length} widget classes, ${Object.keys(manifest.images).length} images in both English and Russian.`
);
