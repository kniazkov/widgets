/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;
import org.jetbrains.annotations.NotNull;

/**
 * The web server that runs web applications, the starting point of the library.
 */
public final class Server {
    /**
     * Starts the web server and the application on it.
     * @param application Application
     */
    public static void start(final @NotNull Application application) {
        Options options = new Options();
        Handler handler = new HttpHandler(application, options);

        com.kniazkov.webserver.Server.start(getWebServerOptions(), handler);
    }

    /**
     * Fills in the web server options
     * @return Web server options
     */
    private static com.kniazkov.webserver.Options getWebServerOptions() {
        return new com.kniazkov.webserver.Options();
    }
}
