import fs from "node:fs";
import { describe, expect, it } from "vitest";
import { manifest, readScriptGroup, readPageRuntime } from "./helpers/scripts.js";

describe("browser script manifest", () => {
    it("registers every source once in its scope so packaged features cannot disappear silently", () => {
        const paths = Object.values(manifest).flat();
        expect(new Set(paths).size).toBe(paths.length);
        const directory = new URL("../../main/html/scripts/", import.meta.url);
        for (const group of ["shared", "widgets", "client"]) {
            const files = fs
                .readdirSync(new URL(`${group}/`, directory))
                .filter(name => name.endsWith(".js"))
                .map(name => `${group}/${name}`);
            expect([...manifest[group]].sort()).toEqual(files.sort());
        }
    });

    it("assembles valid classic scripts without runtime imports or a build step", () => {
        expect(() => new Function(readScriptGroup("shared"))).not.toThrow();
        expect(() => new Function(readPageRuntime())).not.toThrow();
    });
});
