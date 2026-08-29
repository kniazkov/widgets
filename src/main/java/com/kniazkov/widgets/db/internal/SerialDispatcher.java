/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes every database operation on one dedicated thread.
 */
final class SerialDispatcher implements AutoCloseable {
    /**
     * Executor.
     */
    private final ExecutorService executor;

    /**
     * Dispatcher thread.
     */
    private volatile Thread thread;

    /**
     * Creates a dispatcher.
     */
    SerialDispatcher() {
        this.executor = Executors.newSingleThreadExecutor(task -> {
            final Thread worker = new Thread(
                () -> {
                    this.thread = Thread.currentThread();
                    task.run();
                },
                "widgets-database"
            );
            worker.setDaemon(true);
            return worker;
        });
    }

    /**
     * Executes a callable and waits for its result.
     *
     * @param action action
     * @param <T> result type
     * @return result
     */
    <T> T call(final Callable<T> action) {
        if (Thread.currentThread() == this.thread) {
            return invoke(action);
        }
        try {
            return this.executor.submit(action).get();
        } catch (final InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Database operation was interrupted", err);
        } catch (final ExecutionException err) {
            final Throwable cause = err.getCause();
            if (cause instanceof RuntimeException exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Database operation failed", cause);
        }
    }

    /**
     * Executes a runnable and waits for completion.
     *
     * @param action action
     */
    void run(final Runnable action) {
        this.call(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Invokes a callable on the dispatcher thread.
     *
     * @param action action
     * @param <T> result type
     * @return result
     */
    private static <T> T invoke(final Callable<T> action) {
        try {
            return action.call();
        } catch (final RuntimeException | Error err) {
            throw err;
        } catch (final Exception err) {
            throw new IllegalStateException("Database operation failed", err);
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }
}
