# Web Widgets

[![Tests](https://github.com/kniazkov/widgets/actions/workflows/tests.yml/badge.svg)](https://github.com/kniazkov/widgets/actions/workflows/tests.yml)

Web Widgets is a server-driven MVC framework for building web interfaces in Java. The UI is
declared as a tree of Java widgets, application state lives in reactive models, and browser events
are handled by Java controllers. The framework synchronizes the widget tree with a small JavaScript
client over HTTP, so a typical application does not need to generate HTML or write client-side
JavaScript.

The project currently targets Java 8 and is distributed as the Maven artifact
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

        Options options = new Options();
        options.port = 8080;
        Server.start(new Application(page), options);
    }
}
```

Open [http://localhost:8080](http://localhost:8080). Static application files are served from the
`www` directory by default; the path can be changed with `Options.wwwRoot`.

Additional runnable examples are available in
[`src/main/java/com/kniazkov/widgets/example`](src/main/java/com/kniazkov/widgets/example).

## Documentation

- [Widget catalog and hierarchy](docs/WIDGETS.md) — every widget class, its inheritance,
  containment rules, and related view types.

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

```bash
mvn clean package
```

To install a development build locally without signing it:

```bash
mvn clean install -Dgpg.skip=true
```

## License

[MIT](LICENSE)
