import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { createServer } from "node:net";
import { delimiter, dirname, join } from "node:path";
import process from "node:process";
import { setTimeout as delay } from "node:timers/promises";
import { fileURLToPath } from "node:url";

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const isWindows = process.platform === "win32";
const classpathFile = join(projectRoot, "target", "e2e-classpath.txt");
let javaServer;

function executableFromHome(environmentVariable, executable) {
    const home = process.env[environmentVariable];
    if (home) {
        const candidate = join(home, "bin", executable);
        if (existsSync(candidate)) {
            return candidate;
        }
    }
    return executable;
}

function run(command, args, options = {}) {
    return new Promise((resolve, reject) => {
        const child = spawn(command, args, {
            cwd: projectRoot,
            stdio: "inherit",
            windowsHide: true,
            ...options
        });
        child.once("error", reject);
        child.once("exit", (code, signal) => {
            if (code === 0) {
                resolve();
                return;
            }
            reject(new Error(
                `${command} exited with ${signal ? `signal ${signal}` : `code ${code}`}`
            ));
        });
    });
}

async function findFreePort() {
    return new Promise((resolve, reject) => {
        const probe = createServer();
        probe.once("error", reject);
        probe.listen(0, "127.0.0.1", () => {
            const address = probe.address();
            probe.close(error => {
                if (error) {
                    reject(error);
                } else {
                    resolve(address.port);
                }
            });
        });
    });
}

async function waitForServer(baseURL) {
    const deadline = Date.now() + 30_000;
    let lastError;

    while (Date.now() < deadline) {
        if (javaServer.exitCode !== null) {
            throw new Error(`The Java E2E server exited with code ${javaServer.exitCode}`);
        }
        try {
            const response = await fetch(baseURL, {
                signal: AbortSignal.timeout(1_000)
            });
            if (response.ok) {
                return;
            }
            lastError = new Error(`Server returned HTTP ${response.status}`);
        } catch (error) {
            lastError = error;
        }
        await delay(100);
    }

    throw new Error(`The Java E2E server did not become ready: ${lastError}`);
}

async function stopServer() {
    if (!javaServer || javaServer.exitCode !== null) {
        return;
    }

    javaServer.kill();
    await Promise.race([
        new Promise(resolve => javaServer.once("exit", resolve)),
        delay(3_000)
    ]);
    if (javaServer.exitCode === null) {
        javaServer.kill("SIGKILL");
    }
}

async function main() {
    const mavenExecutable = isWindows ? "mvn.cmd" : "mvn";
    const maven = executableFromHome("MAVEN_HOME", mavenExecutable);
    await run(
        maven,
        [
            "--batch-mode",
            "--no-transfer-progress",
            "-DskipTests",
            "test-compile",
            "dependency:build-classpath",
            "-Dmdep.includeScope=test",
            "-Dmdep.outputFile=target/e2e-classpath.txt"
        ],
        { shell: isWindows }
    );

    const dependencyClasspath = (await readFile(classpathFile, "utf8")).trim();
    const classpath = [
        join(projectRoot, "target", "test-classes"),
        join(projectRoot, "target", "classes"),
        dependencyClasspath
    ].filter(Boolean).join(delimiter);
    const port = await findFreePort();
    const baseURL = `http://127.0.0.1:${port}`;
    const javaExecutable = executableFromHome(
        "JAVA_HOME",
        isWindows ? "java.exe" : "java"
    );

    javaServer = spawn(
        javaExecutable,
        [
            "-cp",
            classpath,
            "com.kniazkov.widgets.e2e.E2ETestServer",
            String(port)
        ],
        {
            cwd: projectRoot,
            stdio: "inherit",
            windowsHide: true
        }
    );
    javaServer.once("error", error => {
        console.error("Unable to start the Java E2E server:", error);
    });

    try {
        await waitForServer(baseURL);
        await run(
            process.execPath,
            [join(projectRoot, "node_modules", "@playwright", "test", "cli.js"), "test"],
            {
                env: {
                    ...process.env,
                    WIDGETS_E2E_BASE_URL: baseURL
                }
            }
        );
    } finally {
        await stopServer();
    }
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
