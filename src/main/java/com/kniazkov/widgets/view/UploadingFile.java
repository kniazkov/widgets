/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.webserver.ContentType;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Bounded disk-backed upload descriptor. Register onLoad during onSelect, before completion.
 * Cancellation and callback return release framework-owned content, streams and reservations.
 */
public class UploadingFile {
    /**
     * Immutable owner, including after widget detachment.
     */
    private final RootWidget root;
    /**
     * Selecting widget.
     */
    private final FileLoader loader;
    /**
     * Shared application budget.
     */
    private final UploadRegistry registry;
    /**
     * Owner configuration snapshot.
     */
    private final Options options;
    /**
     * Browser-local upload ID.
     */
    private final int id;
    /**
     * Original display name, never a storage path.
     */
    private final String name;
    /**
     * Declared/inferred MIME type.
     */
    private final String type;
    /**
     * Declared byte reservation.
     */
    private final int size;
    /**
     * Expected chunk count.
     */
    private final int totalChunks;
    /**
     * One bit per accepted chunk, with data stored only on disk.
     */
    private BitSet received = new BitSet();
    /**
     * Absolute monotonic start time, not extended by duplicate chunks or polling.
     */
    private final long started;
    /**
     * Private temporary file, allocated lazily on the first valid chunk.
     */
    private Path path;
    /**
     * Open receiving handle, closed before handing the file to application code.
     */
    private RandomAccessFile output;
    /**
     * Scoped callback content, cleared after cleanup.
     */
    private UploadedFile uploaded;
    /**
     * Completion callback registered before binary completion.
     */
    private Controller<UploadedFile> onLoadCtrl = Controller.stub();
    /**
     * Optional progress model retained without file contents.
     */
    private Model<Integer> percentage;
    /**
     * Accepted chunk count.
     */
    private int count;
    /**
     * First missing chunk index.
     */
    private int next;
    /**
     * Binary completion is separate from callback lifetime.
     */
    private boolean complete;
    /**
     * Terminal cancellation/disposal flag.
     */
    private boolean cancelled;
    /**
     * Queued/running application work; cancelled on expiration or disposal.
     */
    private Future<?> task;

    /**
     * Validates metadata before allocating any file, bitmap or reservation.
     */
    UploadingFile(final FileLoader loader, final UploadEvent event,
            final UploadRegistry registry, final long started) {
        this.root = loader.getRootWidget().orElseThrow(
            () -> new IllegalArgumentException("Detached upload target"));
        this.options = this.root.getOptions();
        validate(event, this.options);
        this.loader = loader;
        this.registry = registry;
        this.started = started;
        this.id = event.fileId;
        this.name = event.name;
        final int dot = event.name.lastIndexOf('.');
        this.type = event.type == null || event.type.isEmpty()
            ? ContentType.fromExtension(dot >= 0 ? event.name.substring(dot + 1) : "").getValue()
            : event.type;
        this.size = event.size;
        this.totalChunks = event.totalChunks;
    }

    /**
     * Returns display name.
     * @return display name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns MIME type.
     * @return MIME type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns declared byte count.
     * @return declared byte count
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Whether the configured processor refused this upload at binary completion.
     */
    private boolean processingBusy;

    /**
     * Worker currently delivering this descriptor's callback, if any.
     */
    private Thread deliveryThread;

    /**
     * Applies a short completion update only while this delivery and page are still live.
     * Call synchronously from onLoad, after background work and before returning.
     *
     * @param action page-owned widget/model changes
     * @return whether the update was applied
     */
    public boolean executeOnRoot(final Runnable action) {
        java.util.Objects.requireNonNull(action, "action");
        final boolean[] applied = {false};
        this.root.execute(() -> {
            if (this.isDelivering() && this.loader.getRootWidget().orElse(null) == this.root) {
                action.run();
                applied[0] = true;
            }
        });
        return applied[0];
    }

    /**
     * @return whether the callback data is still owned by this live delivery
     */
    private synchronized boolean isDelivering() {
        return !this.cancelled && this.deliveryThread == Thread.currentThread();
    }

    /**
     * @return whether the business callback could not be queued
     */
    synchronized boolean isProcessingBusy() {
        return this.processingBusy;
    }

    /**
     * Registers one bounded background completion callback. For page-owned state use
     * {@link #executeOnRoot(Runnable)} after background work; never mutate it on the worker.
     * @param ctrl callback; file access ends when it returns or the upload is cancelled/expired
     */
    public synchronized void onLoad(final Controller<UploadedFile> ctrl) {
        if (this.complete || this.cancelled) {
            throw new IllegalStateException("Upload is already terminal");
        }
        this.onLoadCtrl = java.util.Objects.requireNonNull(ctrl);
    }

    /**
     * Returns progress model; completion does not imply business processing succeeded.
     * @return progress model; completion does not imply business processing succeeded
     */
    public synchronized Model<Integer> getLoadingPercentageModel() {
        if (this.percentage == null) {
            this.percentage = new IntegerModel();
            this.percentage.setData((int) ((long) this.count * 100 / this.totalChunks));
        }
        return this.percentage;
    }

    /**
     * Accepts out-of-order chunks without keeping file-sized arrays in memory.
     */
    synchronized boolean handleUploadChunk(final int index, final byte[] data) {
        if (this.cancelled || this.complete) {
            return false;
        }
        if (data == null || index < 0 || index >= this.totalChunks
                || data.length != this.expectedChunkSize(index)) {
            this.cancel();
            return false;
        }
        try {
            if (this.output == null) {
                Files.createDirectories(this.options.getUploadLimits().directory());
                this.path = Files.createTempFile(this.options.getUploadLimits().directory(),
                    "widgets-upload-", ".tmp");
                this.output = new RandomAccessFile(this.path.toFile(), "rw");
            }
            this.output.seek((long) index * this.options.getChunkSize());
            if (this.received.get(index)) {
                final byte[] previous = new byte[data.length];
                this.output.readFully(previous);
                if (!Arrays.equals(previous, data)) {
                    this.cancel();
                    return false;
                }
                return true;
            }
            this.output.write(data);
            this.received.set(index);
            this.count++;
            this.next = this.received.nextClearBit(this.next);
            if (this.percentage != null) {
                this.percentage.setData((int) ((long) this.count * 100 / this.totalChunks));
            }
            if (this.count == this.totalChunks) {
                this.output.close();
                this.output = null;
                this.complete = true;
                this.received = new BitSet();
                this.uploaded = new UploadedFile(this.name, this.type, this.path, this.size);
                if (!this.registry.completed(this, index, data)) {
                    this.processingBusy = true;
                    this.cancel();
                    return false;
                }
            }
            return true;
        } catch (final IOException error) {
            UploadRegistry.LOGGER.log(Level.WARNING, "Upload storage failed", error);
            this.cancel();
            return false;
        } catch (final RuntimeException | Error error) {
            this.cancel();
            throw error;
        }
    }

    /**
     * Returns cumulative binary progress.
     * @return cumulative binary progress
     */
    synchronized int getNextMissingChunk() {
        return this.next;
    }

    /**
     * Returns whether all chunks were accepted.
     * @return whether all chunks were accepted
     */
    synchronized boolean isComplete() {
        return this.complete;
    }

    /**
     * Explicit cancellation also interrupts queued/running callbacks; it cannot undo effects.
     */
    public synchronized void cancel() {
        this.cancelled = true;
        this.registry.forget(this);
        if (this.task != null) {
            this.task.cancel(true);
        }
        this.release();
    }

    /**
     * Invokes application code outside descriptor/registry/root locks, at most once.
     */
    void deliver() {
        final UploadedFile file;
        final Controller<UploadedFile> callback;
        synchronized (this) {
            if (this.cancelled || this.root.areUploadsClosed()
                    || this.loader.getRootWidget().orElse(null) != this.root) {
                this.cancel();
                return;
            }
            file = this.uploaded;
            this.deliveryThread = Thread.currentThread();
            callback = this.onLoadCtrl;
            this.onLoadCtrl = Controller.stub();
        }
        try {
            callback.handleEvent(file);
        } catch (final RuntimeException | Error error) {
            UploadRegistry.LOGGER.log(Level.SEVERE, "Upload callback failed", error);
        } finally {
            synchronized (this) {
                this.deliveryThread = null;
                this.release();
            }
        }
    }

    /**
     * Closes all library-owned resources; failed deletions remain charged for cleanup retry.
     */
    private void release() {
        try {
            if (this.output != null) {
                this.output.close();
                this.output = null;
            }
            if (this.uploaded != null) {
                this.uploaded.close();
                this.uploaded = null;
            }
            if (this.path != null) {
                Files.deleteIfExists(this.path);
                this.path = null;
            }
            this.received = new BitSet();
            this.onLoadCtrl = Controller.stub();
            this.registry.released(this);
        } catch (final IOException error) {
            this.cancelled = true;
            UploadRegistry.LOGGER.log(Level.WARNING, "Upload resource cleanup failed", error);
        }
    }

    /**
     * Returns whether cleanup should terminate this upload.
     * @return whether cleanup should terminate this upload
     */
    synchronized boolean expired(final long now) {
        return this.cancelled || this.root.areUploadsClosed()
            || this.loader.getRootWidget().orElse(null) != this.root
            || now - this.started >= TimeUnit.MILLISECONDS.toNanos(
                this.options.getUploadLimits().lifetimeMillis());
    }

    /**
     * @param task bounded completion work handle
     */
    synchronized void setTask(final Future<?> task) {
        this.task = task;
    }

    /**
     * Returns immutable owning root.
     * @return immutable owning root
     */
    RootWidget owner() {
        return this.root;
    }

    /**
     * Returns immutable selecting widget.
     * @return immutable selecting widget
     */
    FileLoader loader() {
        return this.loader;
    }

    /**
     * Returns immutable browser-local ID.
     * @return immutable browser-local ID
     */
    int id() {
        return this.id;
    }

    /**
     * Computes chunk boundaries without integer multiplication overflow.
     */
    private int expectedChunkSize(final int index) {
        return index < this.totalChunks - 1 ? this.options.getChunkSize()
            : (int) (this.size - (long) index * this.options.getChunkSize());
    }

    /**
     * Rejects malformed metadata before allocating storage or reserving quotas.
     */
    private static void validate(final UploadEvent event, final Options options) {
        if (event == null || event.fileId <= 0 || event.name == null || event.name.isEmpty()
                || event.name.length() > 255 || event.name.indexOf('/') >= 0
                || event.name.indexOf('\\') >= 0 || event.name.indexOf('\0') >= 0
                || event.size < 0 || event.size > options.getMaxFileSize()
                || (event.type != null && event.type.length() > 255)) {
            throw new IllegalArgumentException("Invalid upload metadata");
        }
        final int chunks = (int) Math.max(1L,
            ((long) event.size + options.getChunkSize() - 1) / options.getChunkSize());
        if (event.totalChunks != chunks) {
            throw new IllegalArgumentException("Invalid upload chunk count");
        }
    }

    /**
     * Returns fixed-size SHA-256 proof; never retains the supplied data.
     * @return fixed-size SHA-256 proof; never retains the supplied data
     */
    static byte[] digest(final byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (final NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }
}
