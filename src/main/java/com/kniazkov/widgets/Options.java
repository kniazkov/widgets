/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Options used when starting the server.
 */
public final class Options implements Cloneable {
    /**
     * Logger for logging server messages.
     */
    public Logger logger = DefaultLogger.INSTANCE;

    /**
     * Maximum client lifetime without upgrading (in ms). If there are no requests from a client (i.e. a web page)
     * during this time, it is considered closed and the watchdog will destroy the client. A longer time means
     * a more stable data exchange process (since it all depends on the reliability of the Internet connection),
     * but a large number of zombie clients wastes memory.
     */
    public long clientLifetime = 3 * 60 * 1000;

    @Override
    public Options clone() {
        Options copy = new Options();
        copy.logger = this.logger;
        copy.clientLifetime = this.clientLifetime;
        return copy;
    }
}
