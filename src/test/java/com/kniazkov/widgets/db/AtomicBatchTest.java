/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.persistence.ChangeSet;
import com.kniazkov.widgets.db.persistence.DatabaseMetadata;
import com.kniazkov.widgets.db.persistence.DatabaseSnapshot;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.json.JsonPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.JdbcPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.SqliteDialect;
import com.kniazkov.widgets.db.query.Query;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Cross-store commit, observer and concurrency contracts.
 */
public final class AtomicBatchTest {
    /**
     * Shared quantity field.
     */
    public static final Field<Integer> QUANTITY = new Field<>(ValueType.INTEGER, "quantity");

    /**
     * Temporary persistence files.
     */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * @param persistence backend
     * @return database with checkout-like stores
     */
    public static Database open(final Persistence persistence) {
        return Database.builder().persistence(persistence)
            .store("orders", Schema.of(QUANTITY))
            .store("lines", Schema.of(QUANTITY))
            .store("cart", Schema.of(QUANTITY)).build();
    }

    /**
     * @param database database
     * @param store store name
     * @param quantity quantity
     * @return isolated draft
     */
    public static Draft draft(final Database database, final String store, final int quantity) {
        final Draft result = database.getStore(store).createDraft();
        result.model(QUANTITY).setData(quantity);
        return result;
    }

    /**
     * JSON and SQLite reopen the complete order and emptied cart.
     * @throws Exception on filesystem failure
     */
    @Test
    public void persistsBatchAndPublishesFinalViews() throws Exception {
        for (final String backend : List.of("json", "sqlite")) {
            final Path directory = this.temporary.newFolder(backend).toPath();
            try (Database database = open(backend(directory, backend))) {
                final DataRecord cart = draft(database, "cart", 2).commit();
                final LiveRecordSet orders = database.getStore("orders").query(Query.all());
                final LiveRecordSet lines = database.getStore("lines").query(Query.all());
                final LiveRecordSet basket = database.getStore("cart").query(Query.all());
                final AtomicInteger events = new AtomicInteger();
                final RecordSetListener listener = change -> {
                    assertEquals(1, orders.getRecords().size());
                    assertEquals(2, lines.getRecords().size());
                    assertTrue(basket.getRecords().isEmpty());
                    events.incrementAndGet();
                };
                orders.subscribe(listener);
                lines.subscribe(listener);
                basket.subscribe(listener);
                database.getStore("lines").getRecordCountModel().addListener(count -> {
                    assertEquals(Integer.valueOf(2), count);
                    assertEquals(1, orders.getRecords().size());
                });
                final List<DataRecord> result = database.commit(List.of(
                    draft(database, "orders", 2), draft(database, "lines", 1),
                    draft(database, "lines", 1)), List.of(cart));
                assertEquals(3, result.size());
                assertEquals(4, events.get());
            }
            try (Database reopened = open(backend(directory, backend))) {
                assertEquals(1, reopened.getStore("orders").getRecords().size());
                assertEquals(2, reopened.getStore("lines").getRecords().size());
                assertTrue(reopened.getStore("cart").getRecords().isEmpty());
            }
        }
    }

    /**
     * Stale drafts, duplicate targets and backend failure leave every store unchanged.
     */
    @Test
    public void rejectsBeforePublishing() {
        final BlockingPersistence persistence = new BlockingPersistence();
        try (Database database = open(persistence)) {
            final DataRecord record = draft(database, "cart", 1).commit();
            final Draft stale = record.edit();
            record.model(QUANTITY).setData(2);
            final Draft order = draft(database, "orders", 1);
            assertThrows(ConflictException.class,
                () -> database.commit(List.of(order, stale), List.of()));
            assertThrows(IllegalArgumentException.class,
                () -> database.commit(List.of(order, order), List.of()));
            final Draft cancelled = draft(database, "orders", 1);
            cancelled.cancel();
            assertThrows(IllegalStateException.class,
                () -> database.commit(List.of(cancelled), List.of()));
            assertThrows(IllegalArgumentException.class,
                () -> database.commit(List.of(order), List.of(record), () -> {
                    throw new IllegalArgumentException("Business precondition");
                }));
            assertThrows(IllegalStateException.class,
                () -> database.commit(List.of(order), List.of(record),
                    () -> record.model(QUANTITY).setData(4)));
            assertEquals(Integer.valueOf(2), record.model(QUANTITY).getData());
            persistence.fail = true;
            assertThrows(PersistenceException.class,
                () -> database.commit(List.of(order), List.of(record)));
            assertTrue(database.getStore("orders").getRecords().isEmpty());
            assertEquals(1, database.getStore("cart").getRecords().size());
            persistence.fail = false;
            database.commit(List.of(order), List.of(record));
            assertThrows(IllegalStateException.class, order::commit);
        }
    }

    /**
     * Direct model writes wait for a batch and keep canonical model identity.
     * @throws Exception on scheduling failure
     */
    @Test(timeout = 10000)
    public void serializesDirectWritesAndReads() throws Exception {
        final BlockingPersistence persistence = new BlockingPersistence();
        try (Database database = open(persistence);
            var executor = Executors.newFixedThreadPool(3)) {
            final DataRecord cart = draft(database, "cart", 1).commit();
            final var model = cart.model(QUANTITY);
            final Draft edit = cart.edit();
            edit.model(QUANTITY).setData(2);
            final Draft order = draft(database, "orders", 2);
            persistence.block = true;
            final var batch = executor.submit(() ->
                database.commit(List.of(edit, order), List.of()));
            assertTrue(persistence.entered.await(3, TimeUnit.SECONDS));
            final var write = executor.submit(() -> model.setData(3));
            final var read = executor.submit(model::getData);
            assertFalse(write.isDone());
            assertFalse(read.isDone());
            persistence.release.countDown();
            batch.get(3, TimeUnit.SECONDS);
            write.get(3, TimeUnit.SECONDS);
            read.get(3, TimeUnit.SECONDS);
            assertEquals(Integer.valueOf(3), model.getData());
            assertEquals(3L, cart.getRevision());
        } finally {
            persistence.release.countDown();
        }
    }

    /**
     * Field observers see final revisions and values in every store, retaining models.
     */
    @Test
    public void publishesFieldsTogetherAndRejectsForeignObjects() {
        try (Database database = open(new com.kniazkov.widgets.db.persistence.NoPersistence());
            Database other = open(new com.kniazkov.widgets.db.persistence.NoPersistence())) {
            final DataRecord first = draft(database, "orders", 1).commit();
            final DataRecord second = draft(database, "lines", 1).commit();
            final var firstModel = first.model(QUANTITY);
            final Draft firstEdit = first.edit();
            final Draft secondEdit = second.edit();
            firstEdit.model(QUANTITY).setData(2);
            secondEdit.model(QUANTITY).setData(3);
            final AtomicInteger events = new AtomicInteger();
            final com.kniazkov.widgets.common.Listener<Integer> listener = value -> {
                assertEquals(Integer.valueOf(2), firstModel.getData());
                assertEquals(Integer.valueOf(3), second.model(QUANTITY).getData());
                assertEquals(2L, first.getRevision());
                assertEquals(2L, second.getRevision());
                assertThrows(IllegalStateException.class, second::remove);
                assertThrows(IllegalStateException.class, database::close);
                events.incrementAndGet();
            };
            firstModel.addListener(listener);
            second.model(QUANTITY).addListener(listener);
            database.commit(List.of(firstEdit, secondEdit), List.of());
            assertEquals(2, events.get());
            org.junit.Assert.assertSame(firstModel, first.model(QUANTITY));
            final Draft foreign = draft(other, "orders", 1);
            assertThrows(IllegalArgumentException.class,
                () -> database.commit(List.of(foreign), List.of()));
            final DataRecord foreignRecord = foreign.commit();
            assertThrows(IllegalArgumentException.class,
                () -> database.commit(List.of(), List.of(foreignRecord)));
        }
    }

    /**
     * @param directory persistence directory
     * @param backend backend name
     * @return persistence
     */
    public static Persistence backend(final Path directory, final String backend) {
        return backend.equals("json") ? new JsonPersistence(directory)
            : new JdbcPersistence("jdbc:sqlite:" + directory.resolve("data.db"),
                new SqliteDialect());
    }

    /**
     * Controllable storage boundary for validation and scheduling tests.
     */
    private static final class BlockingPersistence implements Persistence {
        /**
         * Commit has entered storage.
         */
        private final CountDownLatch entered = new CountDownLatch(1);
        /**
         * Releases storage.
         */
        private final CountDownLatch release = new CountDownLatch(1);
        /**
         * Inject failure before the decision.
         */
        private boolean fail;
        /**
         * Block the next operation.
         */
        private volatile boolean block;

        @Override
        public void initialize(final DatabaseMetadata metadata) {
        }

        @Override
        public DatabaseSnapshot load() {
            return DatabaseSnapshot.empty();
        }

        @Override
        public void commit(final ChangeSet changes) {
            if (this.fail) {
                throw new PersistenceException("Injected storage failure");
            }
            if (this.block) {
                this.entered.countDown();
                try {
                    if (!this.release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out");
                    }
                } catch (final InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
            }
        }

        @Override
        public void close() {
        }
    }
}
