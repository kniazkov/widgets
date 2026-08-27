# Code style

This document describes the conventions used by the current implementation. New Java code must
pass `mvn verify`; rules that can be checked mechanically are enforced by Checkstyle and the Java
compiler. JavaScript code must pass `npm run check:js`, which runs ESLint and Prettier.

All Java lint categories except `try` are treated as errors. The `try` category is excluded because
it reports false positives for test fixtures whose cleanup can propagate `InterruptedException`.

## General formatting

- Use UTF-8, Unix line endings, four spaces for Java indentation, two spaces for JavaScript, and no
  tabs.
- Keep Java lines at or below 100 characters.
- Do not put more than one statement on a line.
- Use braces for every control-flow body, including single-statement bodies.
- Do not use wildcard imports or trailing comments.
- Declare parameters and local variables `final` unless reassignment is part of the algorithm.

The repository contains an `.editorconfig` file with the editor-level rules.

## Naming and structure

- Use standard Java naming: `UpperCamelCase` for types, `lowerCamelCase` for methods and variables,
  and `UPPER_SNAKE_CASE` for constants.
- Prefer one top-level type per source file and give the file the same name as that type.
- Keep implementation details package-private or private. Expose only behavior that belongs to the
  public API.
- Prefer immutable values. Copy mutable input and do not expose internal mutable collections or
  arrays.
- Use `Optional` only as a return type. Do not use it for fields or parameters.
- Avoid `null` in the public API. Reject invalid arguments at the boundary.

## Documentation and comments

- Add Javadocs to types, fields, constructors, and methods, including tests.
- Document API contracts, edge cases, ownership, and thrown exceptions rather than restating the
  implementation.
- Every Javadoc or explanatory comment occupies at least three lines. Do not use `//` comments or
  one-line block comments.
- Keep test Javadocs short and describe the behavior being verified.

For example:

```java
/**
 * The maximum accepted size of a short framework request.
 */
private static final int MAX_REQUEST_SIZE = 1024 * 1024;
```

## Public API

- Preserve source and behavioral compatibility unless a breaking change is deliberate and
  documented.
- Prefer interfaces for public abstractions and immutable value objects for configuration.
- Validate input at the public boundary and keep protocol details out of the exported API.
- A widget or model change must preserve per-client ownership and reactive update semantics.

## Builders

- A builder validates its complete state in `build()` and either returns a usable immutable object
  or throws the documented exception.
- Fluent setters return the same builder instance.
- A built value is a snapshot: later builder mutations must not change it.
- Copy mutable input, including arrays and collections, before retaining it.

## Errors and resources

- Never swallow an exception unless the operation is explicitly best-effort and a nearby block
  comment explains why no recovery is possible.
- Use try-with-resources for owned resources. Define and document ownership at API boundaries.
- Do not expose request data, internal protocol state, or parser diagnostics in client-facing
  errors.

## Tests

- Add unit tests for normal, boundary, and failure behavior.
- Add end-to-end tests when behavior depends on the Java-to-browser round trip, HTTP requests, file
  uploads, or browser-visible semantics.
- Tests must be deterministic and must release servers, executors, temporary files, and other
  resources.
- Run the complete local gate with `mvn verify` and `npm run test:browser`.

## Suppressions

Do not weaken a repository-wide rule to accommodate one exceptional construct. Use the narrowest
possible suppression, explain it with a multi-line comment, and keep the suppressed region small.
