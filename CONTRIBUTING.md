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

## Pull requests

- Explain the user-visible behavior and important design decisions.
- Link related issues when applicable.
- Keep commits reviewable and avoid unrelated formatting or generated files.
- Preserve compatibility unless the pull request explicitly proposes and documents a breaking
  change.
- Wait for both Java and browser GitHub Actions jobs to pass.

Signing is reserved for release builds through the Maven `release` profile. Contributors do not
need a GPG key to run the normal build or verification commands.
