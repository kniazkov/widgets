/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable upload budgets shared by all clients of one Application.
 *
 * @param filesPerClient maximum selected/receiving/processing files per client
 * @param filesPerApplication maximum files across all application clients
 * @param reservedBytes maximum sum of declared sizes, including processing callbacks
 * @param lifetimeMillis absolute lifetime from selection through callback completion
 * @param receipts maximum lightweight completion receipts across the application
 * @param receiptLifetimeMillis retention after binary completion
 * @param directory private temporary storage, outside public static roots
 */
public record UploadLimits(int filesPerClient, int filesPerApplication, long reservedBytes,
        long lifetimeMillis, int receipts, long receiptLifetimeMillis, Path directory) {
    /**
     * Rejects unusable budgets and snapshots the private storage path.
     */
    public UploadLimits {
        if (filesPerClient <= 0 || filesPerApplication <= 0 || reservedBytes < 0
                || lifetimeMillis <= 0 || receipts <= 0 || receiptLifetimeMillis <= 0) {
            throw new IllegalArgumentException("Invalid upload limits");
        }
        directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
    }

    /**
     * Returns bounded defaults; selected files and callback work share the same budget.
     * @return bounded defaults; selected files and callback work share the same budget
     */
    public static UploadLimits defaults() {
        return new UploadLimits(16, 64, 256L * 1024 * 1024, 300000, 256, 60000,
            Path.of(System.getProperty("java.io.tmpdir")));
    }
}
