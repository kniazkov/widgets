/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import com.kniazkov.widgets.db.AtomicBatchTest;
import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.persistence.Persistence;
import com.kniazkov.widgets.db.persistence.jdbc.JdbcPersistence;
import com.kniazkov.widgets.db.persistence.jdbc.SqliteDialect;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Kills child JVMs without shutdown hooks at durable storage boundaries.
 */
public final class AtomicCrashTest {
    /**
     * Temporary persistence roots.
     */
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    /**
     * Reopening after each crash yields a complete old or new checkout.
     * @throws Exception on subprocess or filesystem failure
     */
    @Test(timeout = 90000)
    public void crashAndRecoverRepeatedly() throws Exception {
        for (final String phase : List.of("prepared", "decided", "installed:1",
            "installed:2", "installed:3", "cleaned", "sqlmutation", "beforecommit",
            "aftercommit")) {
            final String backend = phase.startsWith("sql") || phase.endsWith("commit")
                ? "sqlite" : "json";
            final Path directory = this.temporary.newFolder().toPath();
            try (Database database = AtomicBatchTest.open(
                AtomicBatchTest.backend(directory, backend))) {
                AtomicBatchTest.draft(database, "cart", 2).commit();
            }
            crash(directory, backend, phase, "commit");
            if (backend.equals("json") && !phase.equals("prepared")
                && !phase.equals("cleaned")) {
                crash(directory, backend, "recovered:1", "recover");
                crash(directory, backend, "recovered:1", "recover");
            }
            final boolean committed = !List.of("prepared", "sqlmutation", "beforecommit")
                .contains(phase);
            for (int attempt = 0; attempt < 2; attempt++) {
                try (Database database = AtomicBatchTest.open(
                    AtomicBatchTest.backend(directory, backend))) {
                    assertEquals(phase, committed ? 1 : 0,
                        database.getStore("orders").getRecords().size());
                    assertEquals(phase, committed ? 1 : 0,
                        database.getStore("lines").getRecords().size());
                    assertEquals(phase, committed ? 0 : 1,
                        database.getStore("cart").getRecords().size());
                }
            }
        }
    }

    /**
     * An uncertain decision fences memory, emits nothing and recovers on reopen.
     * @throws Exception on filesystem failure
     */
    @Test
    public void fencesAfterDecisionFailure() throws Exception {
        final Path directory = this.temporary.newFolder().toPath();
        final AtomicBoolean armed = new AtomicBoolean();
        final JsonPersistence persistence = new JsonPersistence(directory, phase -> {
            if (armed.get() && phase.equals("installed:1")) {
                throw new IllegalStateException("Injected storage failure");
            }
        });
        try (Database database = AtomicBatchTest.open(persistence)) {
            final var cart = AtomicBatchTest.draft(database, "cart", 2).commit();
            final var order = AtomicBatchTest.draft(database, "orders", 2);
            final var line = AtomicBatchTest.draft(database, "lines", 2);
            final var count = database.getStore("orders").getRecordCountModel();
            count.addListener(value -> org.junit.Assert.fail("Uncertain commit was published"));
            armed.set(true);
            org.junit.Assert.assertThrows(
                com.kniazkov.widgets.db.persistence.RecoveryRequiredException.class,
                () -> database.commit(List.of(order, line), List.of(cart)));
            org.junit.Assert.assertThrows(IllegalStateException.class, count::getData);
            org.junit.Assert.assertThrows(IllegalStateException.class, persistence::load);
        }
        try (Database database = AtomicBatchTest.open(new JsonPersistence(directory))) {
            assertEquals(1, database.getStore("orders").getRecords().size());
            assertEquals(1, database.getStore("lines").getRecords().size());
            assertTrue(database.getStore("cart").getRecords().isEmpty());
        }
    }

    /**
     * @param directory database directory
     * @param backend storage implementation
     * @param phase crash point
     * @param operation commit or recovery
     * @throws Exception on process failure
     */
    private static void crash(final Path directory, final String backend, final String phase,
        final String operation) throws Exception {
        final Path output = directory.resolve("child.log");
        final Process process = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"), AtomicCrashTest.class.getName(),
            directory.toString(), backend, phase, operation)
            .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue("Child timed out", process.waitFor(15, TimeUnit.SECONDS));
            assertEquals(Files.readString(output), 73, process.exitValue());
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Runs in a disposable JVM. Runtime.halt bypasses all close and rollback handlers.
     *
     * @param args directory, backend, checkpoint, operation
     * @throws Exception on database failure
     */
    public static void main(final String[] args) throws Exception {
        final Path directory = Path.of(args[0]);
        final String phase = args[2];
        final AtomicBoolean armed = new AtomicBoolean(args[3].equals("recover"));
        final java.util.function.Consumer<String> checkpoint = reached -> {
            if (armed.get() && reached.equals(phase)) {
                Runtime.getRuntime().halt(73);
            }
        };
        final Persistence persistence;
        if (args[1].equals("json")) {
            persistence = new JsonPersistence(directory, checkpoint);
        } else {
            final Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + directory.resolve("data.db"));
            final Connection intercepted = (Connection) Proxy.newProxyInstance(
                AtomicCrashTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("commit")) {
                        checkpoint.accept("beforecommit");
                    }
                    final Object result;
                    try {
                        result = method.invoke(connection, arguments);
                    } catch (final InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                    if (method.getName().equals("commit")) {
                        checkpoint.accept("aftercommit");
                    }
                    if (result instanceof PreparedStatement statement) {
                        return intercept(statement, checkpoint);
                    }
                    return result;
                });
            persistence = new JdbcPersistence(intercepted, new SqliteDialect());
        }
        try (Database database = AtomicBatchTest.open(persistence)) {
            final var order = AtomicBatchTest.draft(database, "orders", 2);
            final var line = AtomicBatchTest.draft(database, "lines", 2);
            armed.set(true);
            database.commit(List.of(order, line), database.getStore("cart").getRecords());
        }
        throw new IllegalStateException("Crash checkpoint was not reached: " + phase);
    }

    /**
     * @param statement real statement
     * @param checkpoint crash hook
     * @return statement which can terminate the process inside an SQL transaction
     */
    private static PreparedStatement intercept(final PreparedStatement statement,
        final java.util.function.Consumer<String> checkpoint) {
        return (PreparedStatement) Proxy.newProxyInstance(AtomicCrashTest.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> {
                try {
                    final Object result = method.invoke(statement, arguments);
                    if (method.getName().equals("executeUpdate")) {
                        checkpoint.accept("sqlmutation");
                    }
                    return result;
                } catch (final InvocationTargetException failure) {
                    throw failure.getCause();
                }
            });
    }
}
