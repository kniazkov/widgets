/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package example;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.ReleaseInfo;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.example.PublicCatalog;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * External consumer compiled against an installed Maven artifact, not source directories.
 */
public final class ConsumerSmoke {
    /**
     * Static entry point.
     */
    private ConsumerSmoke() {
    }

    /**
     * @param args expected Maven release version
     */
    public static void main(final String[] args) throws Exception {
        if (!ReleaseInfo.version().equals(args[0])) {
            throw new AssertionError("Wrong dependency version: " + ReleaseInfo.version());
        }
        try (Application app = new Application(new PublicCatalog(false))) {
            final int port = Server.start(app, new Options.Builder().setPort(0).build()).getPort();
            final HttpClient http = HttpClient.newHttpClient();
            final String origin = "http://localhost:" + port;
            final var page = http.send(HttpRequest.newBuilder(URI.create(origin)).build(),
                HttpResponse.BodyHandlers.ofString());
            if (page.statusCode() != 200 || !page.body().contains("Public catalogue")
                    || !page.body().contains("?widgets-build=" + ReleaseInfo.buildId())) {
                throw new AssertionError("Packaged consumer bootstrap failed");
            }
            for (final String path : new String[]{"/style.css", "/scripts/options.js",
                    "/scripts/navigation.js", "/scripts/page-runtime.js",
                    "/scripts/shared/transport.js"}) {
                final var response = http.send(HttpRequest.newBuilder(URI.create(origin + path
                    + "?widgets-build=" + ReleaseInfo.buildId())).build(),
                    HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 || response.body().isBlank()
                        || response.body().contains("__WIDGETS_BUILD__")) {
                    throw new AssertionError("Missing packaged browser resource: " + path);
                }
            }
            System.out.println("BUILD=" + ReleaseInfo.buildId());
            System.out.println("CONSUMER_OK=" + ReleaseInfo.version());
        }
    }
}
