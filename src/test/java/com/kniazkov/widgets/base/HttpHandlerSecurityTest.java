/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Method;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/** Reproduces security failures at the HTTP boundary. */
public class HttpHandlerSecurityTest {
    /** Temporary static-file hierarchy. */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /** Static-file requests may not escape the configured public directory. */
    @Test
    public void staticFileRequestCannotTraverseOutsideWwwRoot() throws Exception {
        final File root = this.folder.newFolder("www");
        final File secret = this.folder.newFile("secret.txt");
        java.nio.file.Files.write(secret.toPath(), "private".getBytes(StandardCharsets.UTF_8));
        final Options options = new Options();
        options.wwwRoot = root.getAbsolutePath();
        final HttpHandler handler = this.handler(options);
        final Request request = get("../secret.txt");

        final Response response = handler.handle(request);

        assertNull("Path traversal exposed a file outside wwwRoot", response);
    }

    /** Compiled classes and arbitrary resources must not become public static files. */
    @Test
    public void classpathClassIsNotServedAsAStaticResource() {
        final HttpHandler handler = this.handler(new Options());

        final Response response = handler.handle(
            get("/com/kniazkov/widgets/base/Application.class")
        );

        assertNull("HttpHandler exposed a compiled class from the classpath", response);
    }

    /** Page parameters embedded in an inline script must not be able to end that script. */
    @Test
    public void pageParametersCannotBreakOutOfTheBootstrapScript() {
        final HttpHandler handler = this.handler(new Options());
        final Request request = get("/");
        final String payload = "</script><script>window.__widgetsXss=true</script>";
        request.formData.put("payload", payload);

        final Response response = handler.handle(request);

        assertNotNull(response);
        final String html = new String(response.getData(), StandardCharsets.UTF_8);
        assertFalse("A query parameter produced executable markup", html.contains(payload));
    }

    /** Missing external fields must produce an error response instead of a runtime exception. */
    @Test
    public void malformedCreateClientRequestDoesNotCrashTheHandler() {
        final HttpHandler handler = this.handler(new Options());
        final Request request = new Request();
        request.method = Method.POST;
        request.address = "/";
        request.path = "/";
        request.formData.put("action", "new instance");
        request.formData.put("address", "/");
        request.formData.put("mobile", "false");

        try {
            assertNotNull(
                "Malformed requests need an explicit protocol error response",
                handler.handle(request)
            );
        } catch (final RuntimeException error) {
            fail("Malformed external input crashed HttpHandler: " + error);
        }
    }

    /** Creates a handler with a registered index page. */
    private HttpHandler handler(final Options options) {
        final Application application = BaseTestSupport.application((root, context) -> { });
        return new HttpHandler(application, options);
    }

    /** Creates a minimal GET request. */
    private static Request get(final String path) {
        final Request request = new Request();
        request.method = Method.GET;
        request.address = path;
        request.path = path;
        return request;
    }
}
