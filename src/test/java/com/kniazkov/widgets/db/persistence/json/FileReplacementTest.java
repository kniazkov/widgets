/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for reliable replacement of JSON store files.
 */
public final class FileReplacementTest {
    /**
     * Verifies a replacement is retried after a transient access denial.
     *
     * @throws Exception when replacement unexpectedly fails
     */
    @Test
    public void retriesTransientAccessDenial() throws Exception {
        final Path source = Path.of("store.json.tmp");
        final Path target = Path.of("store.json");
        final AtomicInteger attempts = new AtomicInteger();
        final List<Long> delays = new ArrayList<>();

        FileReplacement.replace(
            source,
            target,
            (ignoredSource, ignoredTarget, ignoredOptions) -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new AccessDeniedException(target.toString());
                }
            },
            delays::add
        );

        assertEquals(3, attempts.get());
        assertEquals(List.of(10L, 20L), delays);
    }
}
