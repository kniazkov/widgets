/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Class describing a client instance.
 */
final class Client implements Comparable<Client> {
    /**
     * Identifier.
     */
    private final UId id;

    /**
     * Timer (in ms). When it expires, the client will be destroyed.
     */
    long timer;

    /**
     * Constructor.
     */
    Client() {
        this.id = UId.create();
    }

    /**
     * Returns client's unique identifier.
     * @return Client's identifier.
     */
    UId getId() {
        return this.id;
    }

    @Override
    public int compareTo(@NotNull Client other) {
        return this.id.compareTo(other.id);
    }
}
