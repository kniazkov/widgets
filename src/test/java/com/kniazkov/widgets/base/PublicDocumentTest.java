/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.view.RootWidget;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Anonymous HTTP bootstrap and explicit public-data boundaries.
 */
public class PublicDocumentTest {
    /**
     * HTML is substantive and escaped, without constructing an interactive/private UI.
     */
    @Test
    public void rendersOnlyExplicitPublicContent() throws Exception {
        final AtomicInteger creates = new AtomicInteger();
        final PublicPage page = new PublicPage() {
            @Override
            public PublicDocument publicDocument(final Map<String, String> parameters) {
                assertThrows(UnsupportedOperationException.class, () -> parameters.put("x", "y"));
                return new PublicDocument("Public <title>", "Description \"quoted\"",
                    "https://example.com/catalog", "https://example.com/photo.png",
                    List.of(PublicDocument.heading(1, "Catalogue"),
                        PublicDocument.paragraph(parameters.getOrDefault("q", "Public text")),
                        PublicDocument.paragraph("__DOCUMENT_HEAD__ __PUBLIC_CONTENT__"),
                        PublicDocument.link("Product", "/product?id=1&name=test"),
                        PublicDocument.image("/photo.png", "Photo", 240, 120)));
            }

            @Override
            public void create(final RootWidget root, final PageContext context) {
                creates.incrementAndGet();
            }
        };
        try (Application app = new Application(page)) {
            app.addPage("private", (root, context) -> {
                creates.incrementAndGet();
                throw new AssertionError("GET must not construct private UI");
            });
            final int port = Server.start(app, new Options.Builder().setPort(0)
                .setTitle("Site title")
                .setDescription("Literal {data} __PUBLIC_CONTENT__ </script>")
                .build()).getPort();
            final HttpClient http = HttpClient.newHttpClient();
            final String base = "http://localhost:" + port;
            final var response = http.send(HttpRequest.newBuilder(URI.create(base
                + "/?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            final String html = response.body();
            assertEquals(200, response.statusCode());
            assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
            assertTrue(html.contains("<h1>Catalogue</h1>"));
            assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
            assertFalse(html.contains("<script>alert(1)</script>"));
            assertTrue(html.contains("<title>Public &lt;title&gt;</title>"));
            assertFalse(html.contains("<title>Site title</title>"));
            assertTrue(html.contains("rel=\"canonical\" href=\"https://example.com/catalog\""));
            assertTrue(html.contains("property=\"og:image\""));
            assertTrue(html.contains("__DOCUMENT_HEAD__ __PUBLIC_CONTENT__"));
            assertTrue(html.contains("href=\"/product?id=1&amp;name=test\""));
            assertTrue(html.contains("Literal {data} __PUBLIC_CONTENT__ \\u003c/script>"));
            assertEquals(0, creates.get());
            final String privateHtml = http.send(HttpRequest.newBuilder(URI.create(base
                + "/private?q=secret")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            assertFalse(privateHtml.contains("widgets-initial"));
            assertFalse(privateHtml.contains("rel=\"canonical\""));
            assertTrue(privateHtml.contains("<title>Site title</title>"));
            assertEquals(0, creates.get());
            final ProtocolTestBrowser browser = new ProtocolTestBrowser(port);
            final var created = browser.create(null);
            assertTrue(created.get("publicDocument").toString().contains("Public text"));
            assertEquals(1, creates.get());
        }
    }

    /**
     * URLs and dimensions reject unsafe or ambiguous browser interpretations.
     */
    @Test
    public void rejectsUnsafeSourcesAndCopiesContent() {
        for (final String bad : List.of("javascript:alert(1)", "data:text/html,x", "//evil.test/x",
                "/\\evil.test/x", "https://user:password@example.com/x", " /x", "/x\ny")) {
            assertThrows(bad, IllegalArgumentException.class, () -> PublicDocument.link("x", bad));
        }
        assertThrows(IllegalArgumentException.class, () -> new PublicDocument("", "", "/x", "",
            List.of()));
        assertThrows(IllegalArgumentException.class, () -> PublicDocument.image("/x", "", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> PublicDocument.heading(7, "x"));
        final var blocks = new java.util.ArrayList<PublicDocument.Block>();
        blocks.add(PublicDocument.paragraph("original"));
        final var document = new PublicDocument("", "", "", "", blocks);
        blocks.clear();
        assertEquals(1, document.blocks().size());
        assertThrows(UnsupportedOperationException.class, () -> document.blocks().clear());
    }
}
