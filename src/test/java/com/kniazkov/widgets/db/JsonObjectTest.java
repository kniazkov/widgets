/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.JsonObjectModel;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.json.JsonPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.JdbcPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.H2Dialect;
import com.kniazkov.widgets.db.persistence.jdbc.SqliteDialect;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Mutable JSON isolation, notifications and persistence upgrades.
 */
public final class JsonObjectTest {
    /**
     * Isolated persistence files.
     */
    @Rule public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * Opaque application data.
     */
    private static final Field<JsonObject> DATA = new Field<>(ValueType.JSON_OBJECT, "data");

    /**
     * Reads, writes, derived models and listeners cannot mutate the internal snapshot.
     * @throws Exception on invalid test JSON
     */
    @Test public void copiesNestedData() throws Exception {
        final JsonObject input = Json.parse(
            "{\"nested\":{\"text\":\"Привет\"},\"array\":[1,true,null,{\"x\":2}]}"
        ).toJsonObject();
        final String expected = input.toString();
        final JsonObjectModel model = new JsonObjectModel(input);
        input.get("nested").toJsonObject().addString("text", "changed");
        assertEquals(expected, model.getData().toString());
        final JsonObject read = model.getData();
        read.get("nested").toJsonObject().addString("text", "edited");
        read.get("array").toJsonArray().addString("new");
        assertEquals(expected, model.getData().toString());
        final AtomicInteger calls = new AtomicInteger();
        final Listener<JsonObject> listener = value -> {
            calls.incrementAndGet();
            value.addString("listener", "private");
        };
        model.addListener(listener);
        final String edited = read.toString();
        assertTrue(model.setData(read));
        assertEquals(1, calls.get());
        assertEquals(edited, model.getData().toString());
        assertFalse(model.setData(model.getData()));
        assertFalse(model.setData(null));
        assertEquals(1, calls.get());
        final var derived = model.deriveWithData(read);
        read.addString("later", "change");
        assertEquals(edited, derived.getData().toString());
        assertEquals(edited, model.getData().toString());
        assertEquals("{}", new JsonObjectModel().getData().toString());
    }

    /**
     * Invalid or non-object persisted values fail loudly instead of losing data.
     */
    @Test public void rejectsInvalidObjects() {
        for (final String text : List.of("[]", "null", "true", "42", "{")) {
            assertThrows(IllegalArgumentException.class, () -> ValueType.JSON_OBJECT
                .fromStoredValue(new StoredValue.StringValue(text)));
        }
    }

    /**
     * Added fields default to empty objects and survive reopening all disk backends.
     * @throws Exception on invalid test JSON
     */
    @Test public void persistsAndUpgrades() throws Exception {
        final Field<String> name = new Field<>(ValueType.STRING, "name");
        final JsonObject input = Json.parse(
            "{\"destination\":{\"code\":44},\"items\":[null,true,1.5,\"Москва\"]}"
        ).toJsonObject();
        final String expected = input.toString();
        for (final String backend : List.of("json", "sqlite", "h2")) {
            final Path path = folder.getRoot().toPath().resolve(backend);
            final UUID id;
            try (Database db = Database.builder().persistence(open(backend, path))
                    .store("items", Schema.of(name)).build()) {
                final Draft draft = db.getStore("items").createDraft();
                draft.model(name).setData("old");
                id = draft.commit().getId();
            }
            for (int round = 0; round < 2; round++) {
                try (Database db = Database.builder().persistence(open(backend, path))
                        .store("items", Schema.of(name, DATA)).build()) {
                    final DataRecord record = db.getStore("items").getRecord(id);
                    final var model = record.model(DATA);
                    assertEquals("old", record.model(name).getData());
                    assertEquals(round == 0 ? "{}" : expected, model.getData().toString());
                    final AtomicInteger calls = new AtomicInteger();
                    final Listener<JsonObject> listener = value -> calls.incrementAndGet();
                    model.addListener(listener);
                    assertEquals(round == 0, model.setData(input));
                    assertEquals(round == 0 ? 1 : 0, calls.get());
                    model.getData().createObject("uncommitted").addString("x", "y");
                    assertEquals(expected, model.getData().toString());
                    final Draft edit = record.edit();
                    final JsonObject data = edit.model(DATA).getData();
                    data.addString("draft", "discard");
                    edit.model(DATA).setData(data);
                    assertEquals(expected, model.getData().toString());
                    final Draft other = db.getStore("items").createDraft();
                    assertEquals("{}", other.model(DATA).getData().toString());
                }
            }
        }
    }

    /**
     * @param backend backend name
     * @param path persistence location
     * @return backend instance
     */
    private static Persistence open(final String backend, final Path path) {
        return switch (backend) {
            case "json" -> new JsonPersistence(path);
            case "sqlite" -> new JdbcPersistence("jdbc:sqlite:" + path, new SqliteDialect());
            default -> new JdbcPersistence("jdbc:h2:" + path, new H2Dialect());
        };
    }
}
