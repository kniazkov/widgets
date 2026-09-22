# Contributing

Contributions are welcome. Keep each pull request focused on one change and update tests and
documentation together with the implementation.

## Prerequisites

- JDK 21
- Maven 3
- Node.js 22.13 or newer with npm
- Chromium installed through Playwright for end-to-end tests

After cloning the repository, install the JavaScript dependencies and the matching browser:

```bash
npm ci
npm run install:e2e-browser
```

On Linux, add `-- --with-deps` to the browser installation command when the Playwright system
packages are not installed yet.

## Building and running examples on Linux

Build the project without starting an application:

```bash
./build.sh
```

This runs `mvn clean package`, including Java tests, and writes the ordinary library JAR and
`target/runtime-classpath.txt`. The classpath contains runtime dependencies, excluding test-only
libraries. The build never edits `pom.xml` and does not select a main class or create a fat JAR.
Additional arguments to `build.sh` are forwarded to Maven.

Build and run any class with a `public static void main(String[] args)` entry point:

```bash
./run.sh com.kniazkov.widgets.example.SortableSectionExample
./run.sh com.kniazkov.widgets.example.ZoomDecoratorExample
```

Run one example at a time because both use port 8080. On a phone, open
`http://<server-address>:8080`; the phone must be able to reach the server's port 8080.
The image is served from `/house.png`.

The first argument is the full class name; subsequent arguments are passed unchanged to its
`main` method. A missing class name prints usage without building anything. If the build fails,
the application is not started. `JAVA_HOME`, when set, selects the JDK for Maven and Java;
otherwise both commands are resolved through `PATH`. JDK 21 is required.

Both scripts switch to the repository directory, so they also work when invoked by absolute path
from another directory and `www` resolves correctly. `run.sh` replaces itself with the Java process,
so Ctrl+C and service-manager stop signals reach the application directly.

## Development workflow

1. Create a branch from the current `master`.
2. Make a focused change and add tests that fail without it.
3. Follow [CODE_STYLE.md](CODE_STYLE.md). Do not disable a repository-wide rule to make one change
   pass.
4. Update public documentation and examples when behavior or API contracts change.
5. Run the complete local gates before opening a pull request.

```bash
mvn verify
npm run test:browser
```

`mvn test` is useful while iterating, but it is not the complete Java gate: `mvn verify` also runs
Checkstyle, compiler lint checks, packaging, and Javadoc generation. `npm run test:browser` runs
ESLint, Prettier, Vitest, and the Playwright end-to-end suite.

## Symbolic-link tests on Windows

Some static-file security tests create symbolic links. Ordinary Windows accounts may not
have permission to do that. Each symlink-only test first probes creation in its temporary
directory: if Windows rejects the probe, JUnit reports that test as **skipped**, with the
filesystem error as the reason. Other routing, traversal, caching, and classpath tests still run.
Errors from actual fixtures or security assertions are never converted into skips.

With symlink permission available, the same tests run normally on Windows. To require every
symlink check (and fail if the probe is unavailable), use:

```bash
mvn -Dwidgets.tests.requireSymlinks=true verify
```

Linux always requires symlink support. CI verifies Java on both Linux and Windows; Linux is
the mandatory gate for all symlink confinement checks. No production security check is disabled.

## Pull requests

- Explain the user-visible behavior and important design decisions.
- Link related issues when applicable.
- Keep commits reviewable and avoid unrelated formatting or generated files.
- Preserve compatibility unless the pull request explicitly proposes and documents a breaking
  change.
- Wait for both Java and browser GitHub Actions jobs to pass.

Signing is reserved for release builds through the Maven `release` profile. Contributors do not
need a GPG key to run the normal build or verification commands.
