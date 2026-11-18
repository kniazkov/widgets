import { defineConfig } from "@playwright/test";
import { readFileSync } from "node:fs";
import { delimiter, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../", import.meta.url));
function server(pom, main, port) {
    const directory = resolve(root, pom, "target");
    const classpath = readFileSync(resolve(directory, "classpath.txt"), "utf8").trim();
    return {
        command: `java -cp "${directory}/classes${delimiter}${classpath}" ${main} ${port}`,
        url: `http://127.0.0.1:${port}`,
        timeout: 30000
    };
}
export default defineConfig({
    testDir: ".",
    outputDir: resolve(root, "target/modules-browser"),
    webServer: [
        server("src/test/minimal", "example.Minimal", 17801),
        server("src/test/consumer", "com.kniazkov.widgets.example.AllWidgets", 17802)
    ]
});
