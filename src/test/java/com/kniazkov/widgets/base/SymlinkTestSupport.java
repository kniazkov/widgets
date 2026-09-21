/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.Assume;

/**
 * Probes symlink creation separately from the security assertions under test.
 */
final class SymlinkTestSupport {
    /**
     * No instances.
     */
    private SymlinkTestSupport() {
    }

    /**
     * Skips symlink-only tests on Windows when an isolated creation probe is unavailable.
     * Linux and strict CI runs always fail instead of skipping. Actual fixture creation
     * and application assertions remain outside this exception handler.
     *
     * @param directory temporary test directory
     * @throws IOException unexpected filesystem errors or required symlink support failures
     */
    static void assumeSupported(final Path directory) throws IOException {
        final Path target = Files.createTempFile(directory, "symlink-probe-", ".txt");
        final Path link = target.resolveSibling(target.getFileName() + "-link");
        final boolean optional = System.getProperty("os.name").toLowerCase(Locale.ROOT)
            .startsWith("windows") && !Boolean.getBoolean("widgets.tests.requireSymlinks");
        try {
            checkCreation(() -> Files.createSymbolicLink(link, target), optional);
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(target);
        }
    }

    /**
     * Applies the platform policy only to a probe, without matching localized OS messages.
     *
     * @param probe isolated link creation
     * @param optional whether this platform allows an assumption failure
     * @throws IOException creation failed on a platform requiring symlinks
     */
    static void checkCreation(final Probe probe, final boolean optional) throws IOException {
        try {
            probe.create();
        } catch (final FileSystemException | UnsupportedOperationException failure) {
            if (!optional) {
                throw failure;
            }
            Assume.assumeNoException("Symbolic links unavailable in the Windows test environment",
                failure);
        }
    }

    /**
     * Isolated filesystem operation used to test the assumption policy.
     */
    @FunctionalInterface
    interface Probe {
        /**
         * Creates a probe link.
         *
         * @throws IOException when the filesystem cannot create the link
         */
        void create() throws IOException;
    }
}
