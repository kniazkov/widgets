/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reproduces security failures at the real HTTP boundary.
 */
public class HttpHandlerSecurityTest {
    /**
     * Temporary static-file hierarchy.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * Running server for the current test.
     */
    private com.kniazkov.webserver.Server server;

    /**
     * Stops the Java 21 server, whose accept loop deliberately keeps the JVM alive.
     */
    @After
    public void stopServer() throws Exception {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * Static-file requests may not escape the configured public directory.
     */
    @Test
    public void staticFileRequestCannotTraverseOutsideWwwRoot() throws Exception {
        final File root = this.folder.newFolder("www");
        final File secret = this.folder.newFile("secret.txt");
        java.nio.file.Files.write(secret.toPath(), "private".getBytes(StandardCharsets.UTF_8));
        this.start(root);

        final String response = this.request("GET", "/../secret.txt", null);

        assertFalse("Path traversal exposed a file outside wwwRoot", response.contains("private"));
        assertFalse(
            "Path traversal returned a successful response",
            response.startsWith("HTTP/1.1 200")
        );
    }

    /**
     * Compiled classes and arbitrary classpath resources must not become public static files.
     */
    @Test
    public void classpathClassIsNotServedAsAStaticResource() throws Exception {
        this.start(this.folder.newFolder("www"));

        final String response = this.request(
            "GET",
            "/com/kniazkov/widgets/base/Application.class",
            null
        );

        assertTrue(response.startsWith("HTTP/1.1 404"));
    }

    /**
     * Page parameters embedded in an inline script must not be able to end that script.
     */
    @Test
    public void pageParametersCannotBreakOutOfTheBootstrapScript() throws Exception {
        final Options options = this.start(this.folder.newFolder("www"));
        final String payload = "</script><script>window.__widgetsXss=true</script>";
        final String target = "/page?payload="
            + URLEncoder.encode(payload, StandardCharsets.UTF_8);

        final String response = this.request("GET", target, null);

        assertTrue(response.startsWith("HTTP/1.1 200"));
        assertFalse("A query parameter produced executable markup", response.contains(payload));
        assertTrue(response.contains(
            "configureUploadProtocol(" + options.getChunkSize() + ", "
                + options.getMaxFileSize() + ")"
        ));
    }

    /**
     * Query parameters on the root page must not be mistaken for an action request.
     */
    @Test
    public void rootPageRetainsQueryParameters() throws Exception {
        this.start(this.folder.newFolder("www"));

        final String response = this.request("GET", "/?item=42", null);

        assertTrue(response.startsWith("HTTP/1.1 200"));
        assertTrue(response.contains("item"));
        assertTrue(response.contains("42"));
    }

    /**
     * Missing external fields must produce JSON instead of crashing a request worker.
     */
    @Test
    public void malformedCreateClientRequestDoesNotCrashTheHandler() throws Exception {
        this.start(this.folder.newFolder("www"));
        final String body = "action=new+instance&address=%2F&mobile=false";

        final String response = this.request("POST", "/", body);

        assertTrue(response.startsWith("HTTP/1.1 200"));
        assertTrue(response.contains("Invalid new instance request"));
    }

    /**
     * A configured binary upload chunk must fit the fixed XMLHttpRequest profile unchanged.
     */
    @Test
    public void uploadSizedMultipartRequestIsAccepted() throws Exception {
        final Options options = this.start(this.folder.newFolder("www"));
        final String boundary = "widgets-test-boundary";
        final byte[] chunk = new byte[options.getChunkSize()];
        for (int index = 0; index < chunk.length; index++) {
            chunk[index] = (byte) index;
        }
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, part(boundary, "action", "new instance"));
        write(body, part(boundary, "address", "/"));
        write(body, part(
            boundary,
            "browserId",
            "123e4567-e89b-12d3-a456-426614174000"
        ));
        write(body, part(boundary, "mobile", "false"));
        write(body, "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"chunk\"; filename=\"chunk.bin\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n");
        body.write(chunk);
        write(body, "\r\n--" + boundary + "--\r\n");

        final String response = this.request(
            "POST",
            "/",
            "multipart/form-data; boundary=" + boundary,
            body.toByteArray()
        );

        assertTrue(response.startsWith("HTTP/1.1 200"));
        assertTrue(response.contains("\"id\""));
    }

    /**
     * Raw upload bodies with query metadata must pass through a public-host request unchanged.
     */
    @Test
    public void rawUploadRequestIsAcceptedFromAnExternalHost() throws Exception {
        this.start(this.folder.newFolder("www"));
        final byte[] chunk = new byte[] {1, 2, 3, 4};
        final String target = "/?action=upload%20chunk&client=%231&widget=%239"
            + "&fileId=1&chunkIndex=0&lastUpdate=%230";

        final String response = this.request(
            "POST",
            target,
            "95.165.134.125:8080",
            "application/octet-stream",
            chunk
        );

        assertTrue(response.startsWith("HTTP/1.1 200"));
        assertTrue(response.contains("\"result\""));
    }

    /**
     * Starts the framework on an ephemeral loopback port.
     */
    private Options start(final File root) {
        final Options options = new Options.Builder()
            .setPort(0)
            .setBindAddress(InetAddress.getLoopbackAddress())
            .setWwwRoot(root.getAbsolutePath())
            .setChunkSize(4 * 1024)
            .setMaxFileSize(32 * 1024 * 1024)
            .build();
        final Page page = (widget, context) -> { };
        final Application application = BaseTestSupport.application(page);
        application.addPage("page", page);
        this.server = Server.start(application, options);
        return options;
    }

    /**
     * Sends one connection-closing HTTP request and returns its complete response.
     */
    private String request(final String method, final String target, final String body)
            throws Exception {
        return this.request(
            method,
            target,
            "application/x-www-form-urlencoded",
            body
        );
    }

    /**
     * Sends one HTTP request with an explicit body content type.
     */
    private String request(
            final String method,
            final String target,
            final String contentType,
            final String body) throws Exception {
        return this.request(
            method,
            target,
            contentType,
            body == null ? null : body.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Sends one HTTP request with an arbitrary binary body.
     */
    private String request(
            final String method,
            final String target,
            final String contentType,
            final byte[] body) throws Exception {
        return this.request(method, target, "localhost", contentType, body);
    }

    /**
     * Sends one HTTP request with an explicit Host header and arbitrary binary body.
     */
    private String request(
            final String method,
            final String target,
            final String host,
            final String contentType,
            final byte[] body) throws Exception {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), this.server.getPort())) {
            socket.setSoTimeout(5000);
            final byte[] content = body == null
                ? new byte[0]
                : body;
            final StringBuilder headers = new StringBuilder()
                .append(method).append(' ').append(target).append(" HTTP/1.1\r\n")
                .append("Host: ").append(host).append("\r\n")
                .append("Connection: close\r\n");
            if (body != null) {
                headers.append("Content-Type: ").append(contentType).append("\r\n")
                    .append("Content-Length: ").append(content.length).append("\r\n");
            }
            headers.append("\r\n");
            socket.getOutputStream().write(headers.toString().getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(content);
            socket.getOutputStream().flush();

            try (InputStream input = socket.getInputStream();
                    ByteArrayOutputStream response = new ByteArrayOutputStream()) {
                input.transferTo(response);
                return response.toString(StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * Creates one UTF-8 multipart form field.
     */
    private static String part(final String boundary, final String name, final String value) {
        return "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
            + value + "\r\n";
    }

    /**
     * Writes one ASCII multipart fragment to a binary request buffer.
     */
    private static void write(final ByteArrayOutputStream output, final String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }
}
