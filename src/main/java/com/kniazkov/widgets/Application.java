/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Set;
import java.util.TreeSet;

/**
 * Web application that is executed by the {@link Server}.
 * In order to use this library, you have to create your web application based on this class.
 */
public final class Application {
    /**
     * Set of clients.
     */
    private final Set<Client> clients;

    /**
     * Constructor.
     */
    public Application() {
        this.clients = new TreeSet<>();
    }

    /**
     * Creates a new client instance.
     * @return Unique identifier of the created client
     */
    UId createClient() {
        final Client client = new Client();
        this.clients.add(client);
        return client.getId();
    }
}
