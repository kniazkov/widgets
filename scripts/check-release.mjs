import assert from "node:assert/strict";
import { readFile, mkdir, rm, readdir, appendFile } from "node:fs/promises";
import { execFileSync } from "node:child_process";
import { resolve, join, delimiter } from "node:path";
import { createHash } from "node:crypto";
import process from "node:process";

const pom = await readFile("pom.xml", "utf8");
const version = pom.match(/<artifactId>widgets<\/artifactId>\s*<version>([^<]+)<\/version>/)?.[1];
assert.match(
    version || "",
    /^\d+\.\d+\.\d+$/,
    "Release candidate must have a fixed SemVer version"
);
const tag = process.env.GITHUB_REF_TYPE === "tag" ? process.env.GITHUB_REF_NAME : `v${version}`;
assert.equal(tag, `v${version}`, "Tag and Maven version differ");
const changelog = await readFile("CHANGELOG.md", "utf8");
assert(changelog.includes(`## [${version}]`), "Missing versioned changelog entry");
const consumerPom = await readFile("src/test/consumer/pom.xml", "utf8");
assert(
    consumerPom.includes(`<widgets.version>${version}</widgets.version>`),
    "Consumer version drift"
);
const revision = execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim();
const knownTags = execFileSync("git", ["tag", "--list", `v${version}`], {
    encoding: "utf8"
}).trim();
assert(
    process.env.GITHUB_REF_TYPE === "tag" || !knownTags,
    "Version already has a release tag: bump the version, never overwrite a release"
);
if (process.env.GITHUB_REF_TYPE === "tag") {
    const tagged = execFileSync("git", ["rev-parse", `${tag}^{commit}`], {
        encoding: "utf8"
    }).trim();
    assert.equal(tagged, revision, "Release tag does not identify this checkout");
}
const jarTool = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin", "jar") : "jar";
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin", "java") : "java";
const artifact = resolve(`target/widgets-${version}.jar`);
const entries = execFileSync(jarTool, ["tf", artifact], { encoding: "utf8" }).trim().split(/\r?\n/);
assert(entries.includes("com/kniazkov/widgets/base/ReleaseInfo.class"));
assert(entries.includes("widgets-release.properties"));
assert(
    !entries.some(entry => entry.includes("/e2e/") || entry.endsWith("Test.class")),
    "Test classes leaked into release"
);
for (const classifier of ["sources", "javadoc"]) {
    const content = await readFile(`target/widgets-${version}-${classifier}.jar`);
    assert(content.length > 0, `Missing ${classifier} artifact`);
}
const directory = resolve("target/release-inspection");
await rm(directory, { recursive: true, force: true });
await mkdir(directory, { recursive: true });
execFileSync(jarTool, ["xf", artifact], { cwd: directory });
async function resourceFiles(directory, prefix = "") {
    const output = [];
    for (const item of await readdir(directory, { withFileTypes: true })) {
        const name = prefix + item.name;
        if (item.isDirectory())
            output.push(...(await resourceFiles(join(directory, item.name), name + "/")));
        else output.push(name);
    }
    return output;
}
for (const path of await resourceFiles("src/main/html")) {
    assert(entries.includes(path), `Missing packaged resource ${path}`);
    assert.deepEqual(
        await readFile(join(directory, path)),
        await readFile(join("src/main/html", path)),
        `Changed resource ${path}`
    );
}
const descriptor = await readFile(join(directory, "widgets-release.properties"), "utf8");
assert(descriptor.includes(`version=${version}`));
assert(!descriptor.includes("${"), "Unfiltered Maven release metadata");
const httpProtocol = descriptor.match(/^httpProtocol=(\d+)$/m)?.[1];
const updateProtocol = descriptor.match(/^updateProtocol=(\d+)$/m)?.[1];
assert(
    (await readFile("src/main/html/scripts/shared/transport.js", "utf8")).includes(
        `widgetsProtocolVersion = "${httpProtocol}"`
    )
);
assert(
    (await readFile("src/main/html/scripts/client/protocol.js", "utf8")).includes(
        `updateStreamVersion = "${updateProtocol}"`
    )
);
const classpath = (await readFile("target/runtime-classpath.txt", "utf8")).trim();
const smokeClasses = resolve("src/test/consumer/target/classes");
function smoke(library) {
    const output = execFileSync(
        java,
        [
            "-cp",
            [smokeClasses, library, classpath].join(delimiter),
            "example.ConsumerSmoke",
            version
        ],
        { encoding: "utf8" }
    );
    assert(output.includes(`CONSUMER_OK=${version}`));
    return output.match(/BUILD=([a-f0-9]{64})/)?.[1];
}
const packagedBuild = smoke(artifact);
assert(packagedBuild, "Missing consumer bundle identity");
assert.equal(smoke(resolve("target/classes")), packagedBuild, "Class directory and JAR differ");
// The same version with modified browser bytes must no longer accept the old identity.
await appendFile(join(directory, "style.css"), "\n/* compatibility mutation probe */\n");
assert.notEqual(smoke(directory), packagedBuild, "Changed bundle retained the old identity");
const report = {
    revision,
    version,
    expectedTag: tag,
    tagBuild: process.env.GITHUB_REF_TYPE === "tag",
    httpProtocol,
    updateProtocol,
    build: packagedBuild,
    jarSha256: createHash("sha256")
        .update(await readFile(artifact))
        .digest("hex"),
    resources: entries.length,
    consumer: "passed"
};
await (
    await import("node:fs/promises")
).writeFile("target/release-verification.json", JSON.stringify(report, null, 2) + "\n");
console.log(JSON.stringify(report, null, 2));
