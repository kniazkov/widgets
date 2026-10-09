import fs from "node:fs";

const directory = new URL("../../../main/html/scripts/", import.meta.url);
export const manifest = JSON.parse(fs.readFileSync(new URL("manifest.json", directory), "utf8"));

export function readScript(path) {
    return fs.readFileSync(new URL(path, directory), "utf8");
}

// Use exactly the server's source groups and ordering, including isolated unit-test harnesses.
export function readScriptGroup(group) {
    if (!Object.hasOwn(manifest, group)) throw new Error(`Unknown script group: ${group}`);
    return manifest[group].map(readScript).join("\n");
}

export function readPageRuntime() {
    return `function createPageRuntime(page) {
        ${readScriptGroup("widgets")}
        ${readScriptGroup("client")}
        return { initClient, mainCycle, disposeClient, showClientError };
    }`;
}
