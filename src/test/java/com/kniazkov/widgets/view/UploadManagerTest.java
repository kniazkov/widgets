/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.controller.UploadCommand;
import com.kniazkov.widgets.controller.UploadEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Managed admission, cancellation and duplicate completion contracts.

 */
public final class UploadManagerTest {
    /**
     * Duplicate final chunks never invoke application processing twice.
     */
    @Test
    public void duplicateCompletionIsIdempotent() throws Exception {
        final RootWidget root = new RootWidget(new Options.Builder().build());
        final UploadList list = new UploadList();
        final AtomicInteger count = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(1);
        final UploadManager manager = new UploadManager("Select", list, 2, 10, (key, file) -> {
            count.incrementAndGet();
            done.countDown();
        });
        root.add(list);
        root.add(new Section(manager));
        try {
            manager.handleUploadEvent(event(1, "a.txt", 3));
            assertTrue(manager.handleUploadChunk(1, 0, new byte[3]).get("result").booleanValue());
            assertTrue(done.await(3, TimeUnit.SECONDS));
            manager.handleUploadChunk(1, 0, new byte[3]);
            manager.handleUploadEvent(event(1, "a.txt", 3));
            assertEquals(1, count.get());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Rejected metadata allocates no binary reservation.
     */
    @Test
    public void enforcesTypeSizeAndCount() {
        final RootWidget root = new RootWidget(new Options.Builder().build());
        final UploadList list = new UploadList();
        final UploadManager manager = new UploadManager("Select", list, 2, 10, (key, file) -> { });
        manager.setAcceptedFiles(".txt");
        root.add(list);
        root.add(new Section(manager));
        try {
            manager.handleUploadEvent(event(1, "bad.exe", 3));
            manager.handleUploadEvent(event(2, "large.txt", 11));
            manager.handleUploadEvent(event(3, "valid.txt", 3));
            for (int id = 1; id <= 3; id++) {
                assertFalse(manager.handleUploadChunk(id, 0, new byte[3])
                    .get("result").booleanValue());
            }
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Cancel revokes pending chunks and frees the list slot on remove.
     */
    @Test
    public void cancellationRejectsLateChunks() {
        final RootWidget root = new RootWidget(new Options.Builder().build());
        final UploadList list = new UploadList();
        final UploadManager manager = new UploadManager("Select", list, 1, 10, (key, file) -> { });
        root.add(list);
        root.add(new Section(manager));
        try {
            manager.handleUploadEvent(event(1, "a.txt", 3));
            final UploadCommand command = new UploadCommand();
            command.fileId = 1;
            command.action = "remove";
            manager.command(command);
            assertFalse(manager.handleUploadChunk(1, 0, new byte[3]).get("result").booleanValue());
            manager.handleUploadEvent(event(2, "b.txt", 3));
            assertTrue(manager.handleUploadChunk(2, 0, new byte[3]).get("result").booleanValue());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Processing failures retry under one stable application key.
     */
    @Test
    public void retryPreservesOperationKey() throws Exception {
        final RootWidget root = new RootWidget(new Options.Builder().build());
        final UploadList list = new UploadList();
        final java.util.List<String> keys = new java.util.concurrent.CopyOnWriteArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);
        final UploadManager manager = new UploadManager("Select", list, 1, 10, (key, file) -> {
            keys.add(key);
            if (keys.size() == 1) {
                throw new java.io.IOException("Test failure");
            }
            done.countDown();
        });
        root.add(list);
        root.add(new Section(manager));
        try {
            root.execute(() -> {
                manager.handleUploadEvent(event(1, "a.txt", 3));
                manager.handleUploadChunk(1, 0, new byte[3]);
            });
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (root.getUploadRegistry().getActiveCount() != 0
                    && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(0, root.getUploadRegistry().getActiveCount());
            final UploadEvent retry = event(2, "a.txt", 3);
            retry.retryOf = 1;
            root.execute(() -> {
                manager.handleUploadEvent(retry);
                assertTrue(manager.handleUploadChunk(2, 0, new byte[3])
                    .get("result").booleanValue());
            });
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertEquals(2, keys.size());
            assertEquals(keys.get(0), keys.get(1));
            assertEquals(4, java.util.UUID.fromString(keys.get(0)).version());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * A processor ignoring interruption must finish before retry can be admitted.
     */
    @Test
    public void cancellationCannotOverlapProcessingRetries() throws Exception {
        final RootWidget root = new RootWidget(new Options.Builder().build());
        final UploadList list = new UploadList();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final UploadManager manager = new UploadManager("Select", list, 1, 10, (key, file) -> {
            entered.countDown();
            boolean finished = false;
            while (!finished) {
                try {
                    finished = release.await(3, TimeUnit.SECONDS);
                } catch (final InterruptedException ignored) {
                    /*
                     * Simulate third-party code that does not respond to cancellation.
                     */
                }
            }
        });
        root.add(list);
        root.add(new Section(manager));
        try {
            root.execute(() -> {
                manager.handleUploadEvent(event(1, "a.txt", 3));
                manager.handleUploadChunk(1, 0, new byte[3]);
            });
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            final UploadCommand cancel = new UploadCommand();
            cancel.fileId = 1;
            cancel.action = "cancel";
            final UploadEvent retry = event(2, "a.txt", 3);
            retry.retryOf = 1;
            root.execute(() -> {
                manager.command(cancel);
                manager.handleUploadEvent(retry);
                assertFalse(manager.handleUploadChunk(2, 0, new byte[3])
                    .get("result").booleanValue());
            });
        } finally {
            release.countDown();
            root.closeUploads();
        }
    }

    /**
     * Builds valid one-chunk metadata.
     */
    private static UploadEvent event(final int id, final String name, final int size) {
        final UploadEvent event = new UploadEvent();
        event.fileId = id;
        event.name = name;
        event.type = "text/plain";
        event.size = size;
        event.totalChunks = 1;
        return event;
    }
}
