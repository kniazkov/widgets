/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.io.IOException;
import java.nio.file.FileSystemException;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Verifies skip policy without requiring Windows privileges on the test host.
 */
public class SymlinkTestSupportTest {
    /**
     * An optional Windows probe reports an assumption, preserving the localized reason.
     */
    @Test
    public void skipsUnavailableOptionalProbe() {
        final FileSystemException failure = new FileSystemException("probe", null,
            "Клиент не обладает требуемыми правами");
        final AssumptionViolatedException skipped = assertThrows(AssumptionViolatedException.class,
            () -> SymlinkTestSupport.checkCreation(() -> {
                throw failure;
            }, true));
        assertSame(failure, skipped.getCause());
    }

    /**
     * Required probes and unrelated I/O errors cannot silently skip a test.
     */
    @Test
    public void propagatesRequiredAndUnexpectedFailures() {
        final FileSystemException denied = new FileSystemException("probe");
        assertSame(denied, assertThrows(FileSystemException.class,
            () -> SymlinkTestSupport.checkCreation(() -> {
                throw denied;
            }, false)));
        final IOException other = new IOException("unexpected");
        assertSame(other, assertThrows(IOException.class,
            () -> SymlinkTestSupport.checkCreation(() -> {
                throw other;
            }, true)));
    }

    /**
     * Unsupported operations obey the same policy; successful probes simply return.
     */
    @Test
    public void handlesUnsupportedAndSuccessfulProbes() throws Exception {
        final UnsupportedOperationException failure = new UnsupportedOperationException();
        assertThrows(AssumptionViolatedException.class,
            () -> SymlinkTestSupport.checkCreation(() -> {
                throw failure;
            }, true));
        assertSame(failure, assertThrows(UnsupportedOperationException.class,
            () -> SymlinkTestSupport.checkCreation(() -> {
                throw failure;
            }, false)));
        SymlinkTestSupport.checkCreation(() -> { }, true);
        SymlinkTestSupport.checkCreation(() -> { }, false);
    }
}
