/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets.example;

import com.kniazkov.widgets.Application;
import com.kniazkov.widgets.Options;
import com.kniazkov.widgets.Server;

/**
 * Web application that outputs a string to the browser.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class HelloWorld {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        Application application = new Application();
        Options options = new Options();
        Server.start(application, options);
    }
}
