/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link Database} implementation that stores each registered {@link Store}
 * as a separate JSON file in a filesystem directory.
 * <p>
 * Every store is mapped by name to a file with the same name and the
 * {@code .json} extension inside the configured folder.
 */
public class JsonDatabase extends Database {
    /**
     * The folder where store files are located.
     */
    private final Path folder;

    /**
     * The registered stores indexed by their names.
     */
    private final Map<String, JsonStore> stores;

    /**
     * Creates a JSON-backed database rooted at the specified folder.
     *
     * @param folder the folder containing JSON files for registered stores
     */
    public JsonDatabase(final Path folder) {
        this.folder = folder;
        this.stores = new TreeMap<>();
    }

    /**
     * Registers a store in this database.
     * <p>
     * The store is loaded from the JSON file named {@code name + ".json"} inside
     * the configured folder if such file already exists. Otherwise, an empty store
     * with the specified schema is created.
     *
     * @param name the unique store name
     * @param fields the schema fields supported by the store
     * @return this database instance
     * @throws IllegalStateException if a store with the same name is already registered
     */
    @Override
    public Database registerStore(final String name, final List<Field<?>> fields) {
        if (this.stores.containsKey(name)) {
            throw new IllegalStateException("A store named '" + name + "' already exists");
        }
        final File file = this.folder.resolve(name + ".json").toFile();
        final JsonStore store = JsonStore.load(this, file, fields);
        this.stores.put(name, store);
        return this;
    }

    /**
     * Returns a registered store by name.
     *
     * @param name the store name
     * @return the registered store
     * @throws IllegalStateException if no store with the specified name has been registered
     */
    @Override
    public Store getStore(final String name) {
        final JsonStore store = this.stores.get(name);
        if (store == null) {
            throw  new IllegalStateException("No store named '" + name + "' has been registered");
        }
        return store;
    }

    /**
     * Flushes all registered stores to disk.
     *
     * @return {@code true} if all stores were flushed successfully; {@code false} otherwise
     */
    @Override
    public boolean flush() {
        boolean flag = true;
        for (JsonStore store : stores.values()) {
            flag = flag && store.flush();
        }
        return flag;
    }
}
