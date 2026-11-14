/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.common.UploadLimits;
import com.kniazkov.widgets.common.UploadProtocol;
import com.kniazkov.widgets.controller.UploadEvent;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-wide upload reservations, disk ownership and bounded completion receipts.
 * Metadata locks never invoke application callbacks or acquire widget-root locks.
 */
public final class UploadRegistry implements AutoCloseable {
    /**
     * Registries with expiring resources; empty registries are removed by the sweeper.
     */
    private static final Set<UploadRegistry> LIVE = ConcurrentHashMap.newKeySet();
    /**
     * Cleanup diagnostics contain no submitted filenames or payloads.
     */
    static final Logger LOGGER = Logger.getLogger(UploadRegistry.class.getName());

    static {
        final var sweeper = Executors.newSingleThreadScheduledExecutor(task -> {
            final Thread thread = new Thread(task, "widgets-upload-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        sweeper.scheduleWithFixedDelay(() -> {
            for (final UploadRegistry registry : LIVE) {
                try {
                    registry.cleanup();
                } catch (final RuntimeException error) {
                    LOGGER.log(Level.WARNING, "Upload cleanup failed", error);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Storage and admission policy.
     */
    private final Options options;
    /**
     * Monotonic clock, injectable by package tests.
     */
    private final LongSupplier clock;
    /**
     * Receiving and processing files, both charged against reservations.
     */
    private final Map<Key, UploadingFile> files = new LinkedHashMap<>();
    /**
     * Oldest-first bounded receipts with fixed-size hashes, never file contents.
     */
    private final Map<Key, Receipt> receipts = new LinkedHashMap<>();
    /**
     * Declared bytes reserved before accepting any binary data.
     */
    private long reservedBytes;

    /**
     * Whether the owning application has permanently closed this registry.
     */
    private boolean closed;
    /**
     * Bounded callback workers; idle threads retire instead of retaining unused registries.
     */
    private final ThreadPoolExecutor callbacks;

    /**
     * Configures upload resource ownership.
     * @param options immutable application policy
     */
    public UploadRegistry(final Options options) {
        this(options, System::nanoTime);
    }

    /**
     * Configures upload resource ownership.
     * @param options immutable application policy
     * @param clock monotonic test clock
     */
    UploadRegistry(final Options options, final LongSupplier clock) {
        this.options = options;
        this.clock = clock;
        this.callbacks = new ThreadPoolExecutor(options.getUploadWorkers(),
            options.getUploadWorkers(), 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(options.getUploadQueueCapacity()), task -> {
                final Thread thread = new Thread(task, "widgets-upload-handler");
                thread.setDaemon(true);
                return thread;
            });
        this.callbacks.allowCoreThreadTimeOut(true);
    }

    /**
     * Reserves declared resources atomically across every client of this application.
     */
    synchronized UploadingFile select(final RootWidget root, final FileLoader loader,
            final UploadEvent event) {
        final UploadLimits limits = this.options.getUploadLimits();
        final long count = this.files.keySet().stream().filter(key -> key.root == root).count();
        if (this.closed || root.areUploadsClosed() || count >= limits.filesPerClient()
                || this.files.size() >= limits.filesPerApplication()
                || event.size > limits.reservedBytes() - this.reservedBytes) {
            return null;
        }
        final Key key = new Key(root, loader, event.fileId);
        if (this.files.containsKey(key) || this.receipts.containsKey(key)) {
            return null;
        }
        final UploadingFile file = new UploadingFile(loader, event, this, this.clock.getAsLong());
        this.files.put(key, file);
        this.reservedBytes += event.size;
        LIVE.add(this);
        return file;
    }

    /**
     * Looks up data without retaining a registry lock during disk IO or progress callbacks.
     */
    JsonObject chunk(final RootWidget root, final FileLoader loader, final int id,
            final int index, final byte[] data) {
        this.cleanup();
        final Key key = new Key(root, loader, id);
        final UploadingFile file;
        synchronized (this) {
            final Receipt receipt = this.receipts.get(key);
            if (receipt != null) {
                return receipt.matches(index, data) ? acknowledgement(receipt.chunks, true)
                    : UploadProtocol.rejected();
            }
            file = this.files.get(key);
        }
        if (file == null) {
            return UploadProtocol.rejected();
        }
        synchronized (file) {
            if (!file.handleUploadChunk(index, data)) {
                return file.isProcessingBusy() ? UploadProtocol.processingBusy()
                    : UploadProtocol.rejected();
            }
            return acknowledgement(file.getNextMissingChunk(), file.isComplete());
        }
    }

    /**
     * Stores only the completing chunk's hash and schedules exactly one bounded callback.
     */
    synchronized boolean completed(final UploadingFile file, final int index,
            final byte[] data) {
        if (this.closed || file.owner().areUploadsClosed()) {
            return false;
        }
        try {
            file.setTask(this.callbacks.submit(file::deliver));
        } catch (final RejectedExecutionException busy) {
            return false;
        }
        final Key key = new Key(file.owner(), file.loader(), file.id());
        this.receipts.put(key, new Receipt(index, data.length, UploadingFile.digest(data),
            file.getNextMissingChunk(), this.clock.getAsLong()));
        while (this.receipts.size() > this.options.getUploadLimits().receipts()) {
            this.receipts.remove(this.receipts.keySet().iterator().next());
        }
        return true;
    }

    /**
     * Removes a reservation only after its owned resources were successfully closed.
     */
    synchronized void released(final UploadingFile file) {
        final Key key = new Key(file.owner(), file.loader(), file.id());
        if (this.files.remove(key, file)) {
            this.reservedBytes -= file.getSize();
        }
    }

    /**
     * Cancellation/expiration invalidates even a completion receipt, without replaying work.
     */
    synchronized void forget(final UploadingFile file) {
        this.receipts.remove(new Key(file.owner(), file.loader(), file.id()));
    }

    /**
     * Expires abandoned data even without further browser requests.
     */
    public void cleanup() {
        final ArrayList<UploadingFile> snapshot;
        final long now = this.clock.getAsLong();
        synchronized (this) {
            snapshot = new ArrayList<>(this.files.values());
            this.receipts.entrySet().removeIf(entry -> now - entry.getValue().created
                >= TimeUnit.MILLISECONDS.toNanos(this.options.getUploadLimits()
                    .receiptLifetimeMillis()) || entry.getKey().root.areUploadsClosed()
                || entry.getKey().loader.getRootWidget().orElse(null) != entry.getKey().root);
        }
        for (final UploadingFile file : snapshot) {
            if (file.expired(now)) {
                file.cancel();
            }
        }
        this.callbacks.purge();
        synchronized (this) {
            if (this.files.isEmpty() && this.receipts.isEmpty()) {
                LIVE.remove(this);
            }
        }
    }

    /**
     * Cancels all resources of a disposed client, including detached loaders and queued work.
     * @param root disposed client root
     */
    public void closeRoot(final RootWidget root) {
        final ArrayList<UploadingFile> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(this.files.entrySet().stream()
                .filter(entry -> entry.getKey().root == root).map(Map.Entry::getValue).toList());
            this.receipts.keySet().removeIf(key -> key.root == root);
        }
        for (final UploadingFile file : snapshot) {
            file.cancel();
        }
        this.callbacks.purge();
    }

    /**
     * Stops admitting uploads, cancels owned data and releases callback workers.
     */
    @Override
    public void close() {
        final List<UploadingFile> snapshot;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            snapshot = new ArrayList<>(this.files.values());
            this.receipts.clear();
        }
        for (final UploadingFile file : snapshot) {
            file.cancel();
        }
        this.callbacks.shutdownNow();
        this.cleanup();
    }

    /**
     * Returns selected/receiving/processing reservation count for diagnostics.
     * @return selected/receiving/processing reservation count for diagnostics
     */
    public synchronized int getActiveCount() {
        return this.files.size();
    }

    /**
     * Returns bytes charged to outstanding reservations.
     * @return bytes charged to outstanding reservations
     */
    public synchronized long getReservedBytes() {
        return this.reservedBytes;
    }

    /**
     * Returns bounded lightweight receipt count.
     * @return bounded lightweight receipt count
     */
    public synchronized int getReceiptCount() {
        return this.receipts.size();
    }

    /**
     * Creates a fresh mutable response; the shared rejection singleton remains untouched.
     */
    private static JsonObject acknowledgement(final int next, final boolean complete) {
        final JsonObject response = new JsonObject();
        response.addBoolean("result", true);
        response.addNumber("nextChunk", next);
        response.addBoolean("complete", complete);
        return response;
    }

    /**
     * Upload identity includes the owning root and widget, never browser filename alone.
     * @param root page owner
     * @param loader target widget
     * @param id browser-local file ID
     */
    private record Key(RootWidget root, FileLoader loader, int id) { }

    /**
     * Fixed-size proof for the last accepted chunk, independent of file size/chunk count.
     * @param index completing chunk index
     * @param length completing chunk size
     * @param hash SHA-256 digest
     * @param chunks terminal nextChunk
     * @param created monotonic completion time
     */
    private record Receipt(int index, int length, byte[] hash, int chunks, long created) {
        /**
         * Verifies the exact completing request after a lost response.
         */
        boolean matches(final int candidate, final byte[] data) {
            return candidate == this.index && data != null && data.length == this.length
                && MessageDigest.isEqual(this.hash, UploadingFile.digest(data));
        }
    }
}
