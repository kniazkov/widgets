/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.List;
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
     * Root widget.
     */
    private final RootWidget root;

    /**
     * Constructor.
     */
    Client() {
        this.id = UId.create();
        this.root = new RootWidget();
    }

    /**
     * Returns client's unique identifier.
     * @return Client's identifier.
     */
    UId getId() {
        return this.id;
    }

    /**
     * Collects updates from all widgets to send to the web page.
     * @return List of instruction
     */
    @NotNull List<Instruction> getUpdates() {
        return root.collectUpdates();
    }

    /**
     * Returns root widget.
     * @return Root widget
     */
    RootWidget getRootWidget() {
        return this.root;
    }

    @Override
    public int compareTo(@NotNull Client other) {
        return this.id.compareTo(other.id);
    }
}
