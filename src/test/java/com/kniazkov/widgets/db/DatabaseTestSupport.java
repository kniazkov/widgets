/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.NoPersistence;
import com.kniazkov.widgets.db.persistence.Persistence;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared fixtures for database contract tests.
 */
final class DatabaseTestSupport {
    /**
     * Employee name field.
     */
    static final Field<String> NAME = new Field<>(ValueType.STRING, "name");

    /**
     * Employee age field.
     */
    static final Field<Integer> AGE = new Field<>(ValueType.INTEGER, "age");

    /**
     * Employee activity field.
     */
    static final Field<Boolean> ACTIVE = new Field<>(ValueType.BOOLEAN, "active");

    /**
     * Employee score field.
     */
    static final Field<Double> SCORE = new Field<>(ValueType.REAL, "score");

    /**
     * Employee schema.
     */
    static final Schema SCHEMA = Schema.of(NAME, AGE, ACTIVE, SCORE);

    /**
     * Utility class.
     */
    private DatabaseTestSupport() {
    }

    /**
     * Opens a memory-only fixture.
     *
     * @return fixture
     */
    static Fixture open() {
        return open(new NoPersistence());
    }

    /**
     * Opens a fixture with the supplied persistence backend.
     *
     * @param persistence persistence backend
     * @return fixture
     */
    static Fixture open(final Persistence persistence) {
        final Database database = Database.builder()
            .persistence(persistence)
            .store("employees", SCHEMA)
            .build();
        return new Fixture(database, database.getStore("employees"));
    }

    /**
     * Adds a complete employee record.
     *
     * @param store employee store
     * @param name name
     * @param age age
     * @param active activity flag
     * @param score score
     * @return committed record
     */
    static DataRecord addEmployee(
        final Store store,
        final String name,
        final int age,
        final boolean active,
        final double score
    ) {
        final Draft draft = store.createDraft();
        draft.model(NAME).setData(name);
        draft.model(AGE).setData(age);
        draft.model(ACTIVE).setData(active);
        draft.model(SCORE).setData(score);
        return draft.commit();
    }

    /**
     * Returns record names in iteration order.
     *
     * @param records records
     * @return names
     */
    static List<String> names(final List<DataRecord> records) {
        final List<String> names = new ArrayList<>();
        for (final DataRecord record : records) {
            names.add(record.model(NAME).getData());
        }
        return names;
    }

    /**
     * Open database and its employee store.
     *
     * @param database database
     * @param store employee store
     */
    record Fixture(Database database, Store store) implements AutoCloseable {
        @Override
        public void close() {
            this.database.close();
        }
    }

    /**
     * Controllable persistence backend for behavioral tests.
     */
    static final class RecordingPersistence implements Persistence {
        /**
         * Snapshot returned during load.
         */
        private final DatabaseSnapshot snapshot;

        /**
         * Successfully committed change sets.
         */
        private final List<ChangeSet> commits;

        /**
         * Optional observer invoked before accepting a commit.
         */
        private Consumer<ChangeSet> observer;

        /**
         * Commit failure flag.
         */
        private boolean failCommit;

        /**
         * Close invocation count.
         */
        private int closeCount;

        /**
         * Creates an empty backend.
         */
        RecordingPersistence() {
            this(DatabaseSnapshot.empty());
        }

        /**
         * Creates a backend returning a predefined snapshot.
         *
         * @param snapshot initial snapshot
         */
        RecordingPersistence(final DatabaseSnapshot snapshot) {
            this.snapshot = snapshot;
            this.commits = new ArrayList<>();
            this.observer = ignored -> {
            };
        }

        @Override
        public DatabaseSnapshot load() {
            return this.snapshot;
        }

        @Override
        public void commit(final ChangeSet changes) {
            this.observer.accept(changes);
            if (this.failCommit) {
                throw new com.kniazkov.widgets.db.persistence.PersistenceException(
                    "Synthetic commit failure"
                );
            }
            this.commits.add(changes);
        }

        @Override
        public void close() {
            this.closeCount++;
        }

        /**
         * Selects whether commits fail.
         *
         * @param value failure flag
         */
        void setFailCommit(final boolean value) {
            this.failCommit = value;
        }

        /**
         * Selects an observer invoked before a commit is accepted.
         *
         * @param value observer
         */
        void setObserver(final Consumer<ChangeSet> value) {
            this.observer = value;
        }

        /**
         * Returns successful commits.
         *
         * @return commits
         */
        List<ChangeSet> getCommits() {
            return List.copyOf(this.commits);
        }

        /**
         * Returns the close invocation count.
         *
         * @return count
         */
        int getCloseCount() {
            return this.closeCount;
        }
    }
}
