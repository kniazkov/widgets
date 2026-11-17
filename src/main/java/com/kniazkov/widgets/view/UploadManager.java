/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.Event;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.controller.UploadCommand;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.protocol.SetUploadManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded multi-file upload selector with a separate UploadList.
 * Use one instance per list. Processing runs on the existing bounded upload executor.
 * The processor must be idempotent by key; cancellation cannot undo external effects.
 */
public final class UploadManager extends FileLoader {
    /**
     * Application processing contract; keys survive user retries.
     */
    @FunctionalInterface
    public interface Processor {
        /**
         * Processes temporary content before returning. Copy it to retain it.
         * @param key stable operation key for idempotent application writes
         * @param file callback-scoped file
         * @throws Exception processing failure, shown as a generic retryable error
         */
        void process(String key, UploadedFile file) throws Exception;
    }
    /**
     * Managed destination.
     */
    private final UploadList list;
    /**
     * Maximum retained entries.
     */
    private final int maxFiles;
    /**
     * Maximum size in bytes.
     */
    private final int maxBytes;
    /**
     * Application processor.
     */
    private final Processor processor;
    /**
     * Entries, including terminal receipts until explicitly removed.
     */
    private final Map<Integer, Entry> entries = new LinkedHashMap<>();
    /**
     * Highest consumed metadata ID, including rejected files.
     */
    private int lastId;
    /**
     * Selection currently being admitted by the base loader.
     */
    private Entry admitting;

    /**
     * Builds a manager; add both it and its list to the same live root.
     * @param caption selector caption
     * @param list destination list
     * @param maxFiles retained entry limit, 1..100
     * @param maxBytes per-file limit (also bounded by Options)
     * @param processor bounded background processing
     */
    @SuppressWarnings("this-escape")
    public UploadManager(final String caption, final UploadList list, final int maxFiles,
            final int maxBytes, final Processor processor) {
        super(caption);
        if (maxFiles < 1 || maxFiles > 100 || maxBytes < 0) {
            throw new IllegalArgumentException("Invalid upload limits");
        }
        this.list = Objects.requireNonNull(list);
        this.maxFiles = maxFiles;
        this.maxBytes = maxBytes;
        this.processor = Objects.requireNonNull(processor);
        this.setMultipleInputFlag(true);
        super.onSelect(file -> this.accept(file));
        this.subscribeToEvent(Event.UPLOAD_COMMAND);
        this.onRetire(() -> {
            for (final Entry entry : this.entries.values()) {
                if (entry.file != null) {
                    entry.file.cancel();
                }
            }
            this.entries.clear();
        });
        this.publish(null, "");
    }

    /**
     * Processing is configured in the constructor; replacing admission is unsupported.
     * @param ctrl unused controller
     */
    @Override
    public void onSelect(final Controller<UploadingFile> ctrl) {
        throw new UnsupportedOperationException("Use the UploadManager processor");
    }

    @Override
    public String getType() {
        return "upload manager";
    }

    @Override
    public void handleUploadEvent(final UploadEvent event) {
        if (event == null || event.fileId <= this.lastId) {
            return;
        }
        this.lastId = event.fileId;
        final Entry previous = event.retryOf > 0 ? this.entries.get(event.retryOf) : null;
        if (event.retryOf > 0 && (previous == null || !previous.retryAllowed || previous.running)) {
            return;
        }
        if (previous == null && this.entries.size() >= this.maxFiles) {
            return;
        }
        final Entry entry = new Entry(event.fileId, previous == null
            ? java.util.UUID.randomUUID().toString() : previous.key);
        if (previous != null) {
            this.entries.remove(event.retryOf);
        }
        this.entries.put(event.fileId, entry);
        if (event.name == null || event.type == null || event.size < 0
                || event.size > this.maxBytes || !this.accepts(event.name, event.type)) {
            entry.state = "error";
            this.publish(entry, "File type or size is not allowed");
            return;
        }
        this.admitting = entry;
        try {
            super.handleUploadEvent(event);
        } finally {
            this.admitting = null;
        }
        if (entry.file == null) {
            entry.state = "error";
            entry.retryAllowed = true;
            this.publish(entry, "Upload capacity is exhausted");
        }
    }

    @Override
    public JsonObject handleUploadChunk(final int fileId, final int chunkIndex, final byte[] data) {
        final JsonObject receipt = super.handleUploadChunk(fileId, chunkIndex, data);
        final Entry entry = this.entries.get(fileId);
        if (entry != null && !receipt.get("result").booleanValue()
                && (entry.state.equals("queued") || entry.state.equals("uploading"))) {
            if (entry.file != null) {
                entry.file.cancel();
            }
            entry.file = null;
            entry.state = "error";
            entry.retryAllowed = true;
            this.publish(entry, "Upload rejected");
        }
        return receipt;
    }

    /**
     * Checks extension/MIME hints on the server; not content security validation.
     */
    private boolean accepts(final String name, final String type) {
        final String patterns = this.getAcceptedFiles();
        if (patterns.isBlank() || patterns.equals("*/*")) {
            return true;
        }
        for (final String pattern : patterns.toLowerCase(java.util.Locale.ROOT).split(",")) {
            final String value = pattern.strip();
            if (value.startsWith(".") && name.toLowerCase(java.util.Locale.ROOT).endsWith(value)
                    || value.endsWith("/*")
                        && type.startsWith(value.substring(0, value.length() - 1))
                    || value.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wires a newly admitted file to one entry.
     */
    private void accept(final UploadingFile file) {
        final Entry entry = this.admitting;
        entry.file = file;
        entry.state = "queued";
        this.publish(entry, "");
        file.onLoad(content -> {
            if (!file.executeOnRoot(() -> {
                entry.state = "processing";
                entry.running = true;
                this.publish(entry, "");
            })) {
                return;
            }
            try {
                this.processor.process(entry.key, content);
                file.executeOnRoot(() -> {
                    entry.state = "done";
                    entry.file = null;
                    this.publish(entry, "");
                });
            } catch (final Exception error) {
                file.executeOnRoot(() -> {
                    entry.state = "error";
                    entry.retryAllowed = true;
                    entry.file = null;
                    this.publish(entry, "Processing failed");
                });
                if (error instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                file.owner().execute(() -> {
                    entry.running = false;
                    if (this.entries.get(entry.id) == entry && entry.state.equals("cancelled")) {
                        entry.retryAllowed = true;
                        this.publish(entry, "");
                    }
                });
            }
        });
    }

    /**
     * Executes only commands valid for the current server-owned state.
     */
    public void command(final UploadCommand command) {
        final Entry entry = this.entries.get(command.fileId);
        if (entry == null) {
            return;
        }
        if (command.action.equals("retry")) {
            if (entry.retryAllowed && !entry.running) {
                this.publish(entry, "retry");
            }
        } else if (command.action.equals("cancel") || command.action.equals("remove")) {
            if (entry.file != null) {
                entry.file.cancel();
            }
            entry.file = null;
            if (!entry.state.equals("done")) {
                entry.state = "cancelled";
                entry.retryAllowed = !entry.running;
            }
            this.publish(entry, command.action);
            if (command.action.equals("remove")) {
                this.entries.remove(command.fileId);
            }
        }
    }

    /**
     * Publishes bounded state; binary content is never retained by this model.
     */
    private void publish(final Entry entry, final String message) {
        final JsonObject data = new JsonObject();
        data.addString("list", this.list.getId().toString());
        data.addNumber("maxFiles", this.maxFiles);
        data.addNumber("maxBytes", this.maxBytes);
        if (entry != null) {
            data.addNumber("fileId", entry.id);
            data.addString("state", entry.state);
            data.addString("message", message);
            data.addBoolean("retry", entry.retryAllowed);
        }
        this.pushUpdate(new SetUploadManager(this.getId(), data));
    }

    /**
     * Server-owned entry and stable application key.
     */
    private static final class Entry {
        /**
         * Attempt identifier.
         */
        private final int id;
        /**
         * Stable operation identifier.
         */
        private final String key;
        /**
         * Current processing state.
         */
        private String state = "queued";
        /**
         * Whether a user retry is admitted.
         */
        private boolean retryAllowed;
        /**
         * Prevents a retry overlapping a processor that has not responded to interruption.
         */
        private boolean running;
        /**
         * Active reservation, if any.
         */
        private UploadingFile file;
        /**
         * Creates an entry.
         */
        private Entry(final int id, final String key) {
            this.id = id;
            this.key = key;
        }
    }
}
