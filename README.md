# Web Widgets

[![Tests](https://github.com/kniazkov/widgets/actions/workflows/tests.yml/badge.svg)](https://github.com/kniazkov/widgets/actions/workflows/tests.yml)

Web Widgets is a server-driven MVC framework for building web interfaces in Java. The UI is
declared as a tree of Java widgets, application state lives in reactive models, and browser events
are handled by Java controllers. The framework synchronizes the widget tree with a small JavaScript
client over HTTP, so a typical application does not need to generate HTML or write client-side
JavaScript.

The project targets Java 21 and is distributed as the Maven artifact
`com.kniazkov:widgets:0.1`.

## MVC architecture

- **Model** — typed reactive values from `com.kniazkov.widgets.model`. Models support validation,
  listeners, derived read-only values, cascading overrides, and synchronized wrappers.
- **View** — widgets and styles from `com.kniazkov.widgets.view`. Each browser tab gets its own
  `RootWidget` tree, built by a `Page` on the server.
- **Controller** — event handlers from `com.kniazkov.widgets.controller`. Controllers are attached
  to widgets and handle clicks, text input, pointer events, and file uploads.

When a model changes, its bindings enqueue view updates. The server serializes those updates into
the internal protocol and the browser client applies them to the DOM. Events travel in the other
direction: the browser sends them to the server, the target widget invokes its controller, and the
controller can update one or more models.

## Features

- Server-side widget tree with containers, text, buttons, input fields, tables, images, and file
  uploads
- Reactive data binding and validation
- Reusable, cascading styles with state-specific properties
- Multiple pages and per-client page context
- Built-in image loading and processing utilities
- Lightweight record and JSON persistence APIs

## Quick start

Install the library into your local Maven repository:

```bash
./install.sh
```

Add it to an application:

```xml
<dependency>
    <groupId>com.kniazkov</groupId>
    <artifactId>widgets</artifactId>
    <version>0.1</version>
</dependency>
```

Create a page and start the server:

```java
import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Panel;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

public final class HelloWidgets {
    public static void main(String[] args) {
        Page page = (root, context) -> {
            TextWidget message = new TextWidget("It works!");
            Button button = new Button("Click me");

            button.onClick(event -> message.setText("Hello from Java"));
            root.add(new Panel(
                new Section(message, button)
            ));
        };

        Options options = new Options.Builder()
            .setPort(8080)
            .build();
        Server.start(new Application(page), options);
    }
}
```

Open [http://localhost:8080](http://localhost:8080). Static application files are served from the
`www` directory by default; the path can be changed with `Options.Builder.setWwwRoot(...)`.

Additional runnable examples are available in
[`src/main/java/com/kniazkov/widgets/example`](src/main/java/com/kniazkov/widgets/example).

### Server and HTTPS configuration

`Options` exposes the listener port, bind address, worker count, and immutable HTTPS settings.
Request limits and timeouts are an internal framework profile sized for short XMLHttpRequest
exchanges and 64 KiB upload chunks. For example, a server intended to sit behind a local reverse
proxy can bind only to loopback:

```java
Options options = new Options.Builder()
    .setPort(8080)
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setMaxWorkers(200)
    .build();
```

To run the same widgets application directly over HTTPS, build the webserver's immutable TLS
configuration and pass it through the application options builder:

```java
char[] password = loadPassword();
SslOptions ssl = new SslOptions.Builder()
    .setKeyStoreFile("server.p12")
    .setPassword(password)
    .build();

Options options = new Options.Builder()
    .setPort(8443)
    .setSslOptions(ssl)
    .build();
Server.start(new Application(page), options);
```

`SslOptions` also supports JKS, a PEM certificate chain with an unencrypted PKCS #8 key, explicit
TLS versions and cipher suites, and optional or required mutual TLS. Run separate server instances
on different ports when both HTTP and HTTPS listeners are required. The caller remains responsible
for clearing its original password array after the TLS options have been built.

## Documentation

- [Model catalog and hierarchy](docs/MODELS.md) — every model type, its contract, defaults,
  validation, composition, and synchronization behavior.
- [Widget catalog and hierarchy](docs/WIDGETS.md) — every widget class, its inheritance,
  containment rules, and related view types.
- [Contribution guide](CONTRIBUTING.md) — development workflow and the checks required for a
  pull request.
- [Code style](CODE_STYLE.md) — Java and JavaScript conventions enforced by the build.

## Project layout

| Package | Purpose |
| --- | --- |
| `base` | Application, pages, clients, HTTP handling, and server lifecycle |
| `model` | Reactive values, validation, transformations, and bindings |
| `view` | Widgets, containers, properties, states, and styles |
| `controller` | Browser events and their Java handlers |
| `protocol` | Commands used to synchronize the server-side tree with the browser |
| `db` | Records, stores, filters, and JSON-backed persistence |
| `images` | Image sources, loading, conversion, and processing |
| `common` | Shared value objects and utilities |
| `example` | Small runnable applications demonstrating the API |

Browser runtime resources are stored in `src/main/html` and packaged into the library JAR.

## Building

Development requires:

- JDK 21, with `java` available through `JAVA_HOME` or `PATH`;
- Maven 3, with `mvn` (Linux) or `mvn.cmd` (Windows) on `PATH`;
- Node.js 22.13 or newer and npm for browser-client tests;
- Chromium installed through Playwright for end-to-end tests.

### Java

Compile the project, run unit tests, build artifacts, generate Javadocs, and check Java code style:

```bash
mvn verify
```

Signing is enabled only by the `release` profile. To install a development build locally:

```bash
mvn clean install
```

### Fast JavaScript tests

Vitest and jsdom test browser code without starting Java or a real browser. On Linux:

```bash
npm ci
npm run test:js
```

On Windows PowerShell, invoke the `.cmd` launcher explicitly. This works even when the PowerShell
execution policy blocks `npm.ps1`:

```powershell
npm.cmd ci
npm.cmd run test:js
```

Use `npm run test:js:watch` (or `npm.cmd run test:js:watch` on PowerShell) while developing the
browser client.

### JavaScript style

ESLint rejects `var` and requires `const` whenever a binding is not reassigned. Prettier provides
the shared formatting rules for production scripts, test code, and the end-to-end runner.

Check both rules without changing files on Linux:

```bash
npm run check:js
```

Apply the formatter after editing JavaScript:

```bash
npm run format:js
```

Use the explicit npm launcher in Windows PowerShell:

```powershell
npm.cmd run check:js
npm.cmd run format:js
```

The complete `test:browser` command runs the style checks before Vitest and Playwright, so GitHub
Actions enforces the same rules automatically.

### Full browser/server end-to-end test

The Playwright test runs the real packaged JavaScript in Chromium against a real Java server. It
checks the complete round trip: initial Java widget updates reach the DOM, a browser click is sent
to the Java controller, and the controller's text update returns to the DOM.

Install Chromium once and run the test on Linux:

```bash
npm ci
npm run install:e2e-browser -- --with-deps
npm run test:e2e
```

`--with-deps` installs Chromium's Linux system packages and may request elevated privileges. On
Windows PowerShell, no Linux system-package step is needed:

```powershell
npm.cmd ci
npm.cmd run install:e2e-browser
npm.cmd run test:e2e
```

Run both the fast JavaScript suite and the end-to-end suite with `npm run test:browser` on Linux or
`npm.cmd run test:browser` on Windows.

The cross-platform `scripts/run-e2e.mjs` runner handles the non-trivial lifecycle. It selects
`mvn` or `mvn.cmd`, compiles the test server, builds its test classpath, reserves a loopback port,
starts Java, waits until HTTP is ready, launches Playwright, and stops Java even when the test
fails. Separate `.sh` and `.bat` files are therefore not required.

GitHub Actions performs the same browser test on Linux after installing Chromium and its system
dependencies.

## Contributing

Before opening a pull request, run the complete Java and browser gates:

```bash
mvn verify
npm run test:browser
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the contribution workflow and
[CODE_STYLE.md](CODE_STYLE.md) for the conventions enforced by those commands.

## License

[MIT](LICENSE)
