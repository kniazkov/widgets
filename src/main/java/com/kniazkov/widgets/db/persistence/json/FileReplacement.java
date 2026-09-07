/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Replaces a file and tolerates short-lived locks imposed by Windows software.
 */
final class FileReplacement {
    /**
     * Maximum number of replacement attempts.
     */
    private static final int MAX_ATTEMPTS = 7;

    /**
     * Delay before the second attempt, in milliseconds.
     */
    private static final long INITIAL_DELAY = 10L;

    /**
     * Utility class.
     */
    private FileReplacement() {
    }

    /**
     * Atomically replaces the target when supported by the file system.
     *
     * @param source completed temporary file
     * @param target destination file
     * @throws IOException when replacement ultimately fails
     */
    static void replace(final Path source, final Path target)
        throws IOException {
        replace(source, target, Files::move, Thread::sleep);
    }

    /**
     * Replaces the target using injectable operations for deterministic tests.
     *
     * @param source completed temporary file
     * @param target destination file
     * @param operation file move operation
     * @param pause delay operation
     * @throws IOException when replacement ultimately fails
     */
    static void replace(
        final Path source,
        final Path target,
        final MoveOperation operation,
        final Pause pause
    ) throws IOException {
        long delay = INITIAL_DELAY;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                moveOnce(source, target, operation);
                return;
            } catch (final AccessDeniedException denied) {
                if (attempt == MAX_ATTEMPTS) {
                    throw denied;
                }
                try {
                    pause.apply(delay);
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    final InterruptedIOException failure =
                        new InterruptedIOException(
                            "Interrupted while replacing " + target
                        );
                    failure.initCause(interrupted);
                    failure.addSuppressed(denied);
                    throw failure;
                }
                delay *= 2L;
            }
        }
    }

    /**
     * Performs one atomic replacement with a portable fallback.
     *
     * @param source completed temporary file
     * @param target destination file
     * @param operation file move operation
     * @throws IOException when replacement fails
     */
    private static void moveOnce(
        final Path source,
        final Path target,
        final MoveOperation operation
    ) throws IOException {
        try {
            operation.apply(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (final AtomicMoveNotSupportedException ignored) {
            operation.apply(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    /**
     * File move operation.
     */
    @FunctionalInterface
    interface MoveOperation {
        /**
         * Moves one file.
         *
         * @param source source file
         * @param target destination file
         * @param options move options
         * @throws IOException when the move fails
         */
        void apply(Path source, Path target, CopyOption... options)
            throws IOException;
    }

    /**
     * Delay operation.
     */
    @FunctionalInterface
    interface Pause {
        /**
         * Waits for the specified interval.
         *
         * @param milliseconds delay in milliseconds
         * @throws InterruptedException when the current thread is interrupted
         */
        void apply(long milliseconds) throws InterruptedException;
    }
}
