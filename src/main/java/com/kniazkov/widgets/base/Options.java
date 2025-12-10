/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * Configuration options used when starting the server.
 */
public class Options implements Cloneable {
    /**
     * Maximum lifetime of a client without updates, in milliseconds.
     * If no requests are received from a client (i.e., the browser tab) within this time,
     * the client is considered disconnected and will be removed by the server watchdog.
     * This value should be long enough to account for tabs running in the background,
     * where JavaScript timers are throttled by the browser (often to 1 update per minute).
     * A default of 3 minutes provides a balance between connection reliability and
     * timely cleanup of zombie sessions.
     */
    public long clientLifetime = 3 * 60 * 1000;

    /**
     * Root directory for static files served via HTTP GET.
     * Any request that points to a file path (including direct access from a browser address bar)
     * is resolved relative to this directory. It should contain all project assets intended to be
     * served as static content—HTML, JavaScript, CSS, images, and other public resources.
     */
    public String wwwRoot = "www";

    /**
     * The HTTP port number on which the server will run.
     */
    public int port = 8080;

    @Override
    public Options clone() {
        Options copy = new Options();
        copy.clientLifetime = this.clientLifetime;
        copy.wwwRoot = this.wwwRoot;
        copy.port = this.port;
        return copy;
    }
}
