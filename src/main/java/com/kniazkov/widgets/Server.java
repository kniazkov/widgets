/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
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
        Handler handler = new HttpHandler(application);

        com.kniazkov.webserver.Server.start(options, handler);
    }
}
