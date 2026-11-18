# Your first Widgets application with Maven

[Web Widgets](../../README.md) · [Русский](../ru/quick-start.md)

Start with an empty directory. By the end you will have a standalone Java application with a button
that increments a counter in your browser. You need Git, Bash, a JDK 21 installation and Maven 3.9.
Run `mvn -version` and check that Maven reports Java 21.

## Install Widgets once

This guide uses `com.kniazkov:widgets-http:0.2.0`. This version is available from source
and has yet to be published to Maven Central. From the empty directory, install the web module
from the pinned revision below. Maven also installs its parent POM and the core module.
Once these coordinates are published to Central, you can start at the next section and Maven will
download the dependency declared in your POM.

```bash
git clone https://github.com/kniazkov/widgets.git widgets
git -C widgets checkout --detach d1f627b7c1b17a5e722f7d502db48e32b56e03de
mvn -f widgets/modules/pom.xml -pl :widgets-http -am clean install
```

## Create the application

Create your project alongside the library checkout. All remaining paths are relative to this new project.

```bash
mkdir -p hello-widgets/src/main/java/example
cd hello-widgets
```

Save this file as `pom.xml`. Maven will resolve Widgets from the local repository populated above.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>example</groupId>
    <artifactId>hello-widgets</artifactId>
    <version>1.0</version>
    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.kniazkov</groupId>
            <artifactId>widgets-http</artifactId>
            <version>0.2.0</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.15.0</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.5.0</version>
                <configuration>
                    <mainClass>example.Counter</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Save the page and its entry point together in `src/main/java/example/Counter.java`.

```java
package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.IntegerToStringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;

public class Counter {
    public static void main(String[] args) {
        Page page = (root, context) -> {
            IntegerModel count = new IntegerModel();
            TextWidget value = new TextWidget();
            value.setTextModel(new IntegerToStringModel(count));

            Button button = new Button("Click me");
            button.onClick(event -> count.setData(count.getData() + 1));
            root.add(new Section(button, new TextWidget("Click counter: "), value));
        };
        Server.start(new Application(page), new Options.Builder().setPort(8080).build());
    }
}
```

## Run it

From the directory containing your application's `pom.xml`, run:

```bash
mvn compile exec:java
```

Open [localhost:8080](http://localhost:8080/). The counter starts at **0**. Click **Click me**
twice to see **1** and then **2**. Press Ctrl+C in the terminal to stop the application.

## Which dependency do I need?

- `widgets-http` includes the server and pulls in the widgets, models and browser resources needed for this page.
- Add `widgets-db` for the library's record and persistence APIs, or `widgets-images` for image decoding and processing. To use either at this revision, extend the install command’s `-pl` list (for example, `-pl :widgets-http,:widgets-db`) and add a dependency of the same version to your POM.
- The full `com.kniazkov:widgets` artifact bundles all modules and examples. Use it on its own if you need that distribution. Build it from the same checkout with `mvn -f widgets/pom.xml clean install` from the directory where you cloned Widgets.
