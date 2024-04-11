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
     * @param options Options
     */
    public static void start(final @NotNull Application application, final @NotNull Options options) {
        final Options cOpt = options.clone();
        Handler handler = new HttpHandler(application, cOpt);
        application.setOptions(cOpt);

        com.kniazkov.webserver.Server.start(getWebServerOptions(), handler);
        options.logger.write("Server started.");
    }

    /**
     * Fills in the web server options
     * @return Web server options
     */
    private static com.kniazkov.webserver.Options getWebServerOptions() {
        return new com.kniazkov.webserver.Options();
    }
}
