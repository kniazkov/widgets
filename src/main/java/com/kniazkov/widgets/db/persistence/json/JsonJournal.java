/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db.persistence.json;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonParsingMode;
import com.kniazkov.widgets.db.persistence.PersistenceException;
import com.kniazkov.widgets.db.persistence.RecoveryRequiredException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Redo journal containing complete replacement images, retained until all targets
 * are installed. The atomic rename to database.commit is the commit decision.
 */
final class JsonJournal {
    /**
     * Directory containing the journal and targets.
     */
    private final Path directory;

    /**
     * Deterministic crash checkpoints, normally a no-op.
     */
    private final Consumer<String> checkpoint;

    /**
     * @param directory database directory
     * @param checkpoint crash-test hook
     */
    JsonJournal(final Path directory, final Consumer<String> checkpoint) {
        this.directory = directory;
        this.checkpoint = checkpoint;
    }

    /**
     * Replays a committed journal before any database file is read.
     */
    void recover() {
        final Path decision = this.directory.resolve("database.commit");
        if (!Files.exists(decision)) {
            return;
        }
        try {
            final JsonObject root = Json.parse(decision.toFile(), JsonParsingMode.STRICT)
                .objectValueOrThrow();
            final Map<String, String> images = new LinkedHashMap<>();
            for (final String name : root.keySet()) {
                if (!name.endsWith(".json") || name.contains("/") || name.contains("\\")
                    || !this.directory.resolve(name).getParent().equals(this.directory)) {
                    throw new PersistenceException("Invalid journal target");
                }
                images.put(name, root.get(name).stringValue());
            }
            this.install(images, "recovered:");
        } catch (final IOException | com.kniazkov.json.JsonException | RuntimeException failure) {
            throw new RecoveryRequiredException("Cannot recover JSON database", failure);
        }
    }

    /**
     * Persists a decision, installs all images and removes the decision durably.
     *
     * @param images complete store files
     */
    void commit(final Map<String, String> images) {
        if (images.isEmpty()) {
            return;
        }
        final JsonObject root = new JsonObject();
        images.forEach(root::addString);
        boolean deciding = false;
        try {
            final Path temporary = this.directory.resolve("database.prepare");
            writeForced(temporary, root.toText("  "));
            this.checkpoint.accept("prepared");
            deciding = true;
            Files.move(temporary, this.directory.resolve("database.commit"),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            this.forceDirectory();
            this.checkpoint.accept("decided");
            this.install(images, "installed:");
        } catch (final IOException | RuntimeException failure) {
            if (deciding) {
                throw new RecoveryRequiredException("JSON commit requires recovery", failure);
            }
            throw new PersistenceException("Cannot prepare JSON commit", failure);
        }
    }

    /**
     * @param images replacement images
     * @param phase crash checkpoint prefix
     * @throws IOException when durable installation fails
     */
    private void install(final Map<String, String> images, final String phase)
        throws IOException {
        int index = 0;
        for (final Map.Entry<String, String> image : images.entrySet()) {
            final Path temporary = this.directory.resolve("database.install");
            writeForced(temporary, image.getValue());
            FileReplacement.replace(temporary, this.directory.resolve(image.getKey()));
            this.forceDirectory();
            this.checkpoint.accept(phase + ++index);
        }
        Files.delete(this.directory.resolve("database.commit"));
        this.forceDirectory();
        this.checkpoint.accept("cleaned");
    }

    /**
     * @param file target file
     * @param content complete content
     * @throws IOException when data cannot be flushed
     */
    private static void writeForced(final Path file, final String content) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            final ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    /**
     * Flushes directory entries on Unix. Java does not expose directory flushing on
     * Windows; there the contract covers process crashes, not sudden power failure.
     *
     * @throws IOException when a supported directory flush fails
     */
    private void forceDirectory() throws IOException {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try (FileChannel channel = FileChannel.open(this.directory, StandardOpenOption.READ)) {
                channel.force(true);
            }
        }
    }
}
