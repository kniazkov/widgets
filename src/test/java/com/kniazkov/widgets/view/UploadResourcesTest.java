/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.common.UploadLimits;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.controller.UploadEvent;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

/**
 * Resource lifetimes, aggregate quotas and retries are independent of file contents.
 */
public final class UploadResourcesTest {
    /**
     * Every test uses private storage so leaked files are observable without GC guesses.
     */
    @Rule
    public final TemporaryFolder directory = new TemporaryFolder();

    /**
     * Ten completed files release their data even if the application retains descriptors.
     */
    @Test
    public void releasesCompletedContentAfterCallback() throws Exception {
        final Options options = new Options.Builder().setChunkSize(65536).build();
        final RootWidget root = new RootWidget(options);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final List<UploadedFile> results =
            java.util.Collections.synchronizedList(new ArrayList<>());
        final List<UploadingFile> descriptors = new ArrayList<>();
        final CountDownLatch completed = new CountDownLatch(10);
        loader.onSelect(descriptor -> {
            descriptors.add(descriptor);
            descriptor.onLoad(file -> {
                assertEquals(65536, file.getContent().length);
                results.add(file);
                completed.countDown();
            });
        });
        try {
            for (int id = 1; id <= 10; id++) {
                loader.handleUploadEvent(event(id, 65536, 1));
                assertTrue(loader.handleUploadChunk(id, 0, new byte[65536])
                    .get("complete").booleanValue());
            }
            assertTrue(completed.await(3, TimeUnit.SECONDS));
            await(() -> results.stream().allMatch(UploadResourcesTest::closed));
            assertEquals(10, descriptors.size());
            assertEquals(10, results.size());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Repeated final chunks use bounded receipts; callback file/streams expire after return.
     */
    @Test
    public void keepsOnlyBoundedReceiptsAndClosesStreams() throws Exception {
        final AtomicLong clock = new AtomicLong();
        final Options options = this.options(4, 8, 100, 2);
        final UploadRegistry registry = new UploadRegistry(options, clock::get);
        final RootWidget root = new RootWidget(options, registry);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<InputStream> stream = new AtomicReference<>();
        loader.onSelect(descriptor -> descriptor.onLoad(file -> {
            try {
                stream.set(file.openStream());
                assertArrayEquals(new byte[]{1, 2, 3}, file.getContent());
                calls.incrementAndGet();
            } catch (final java.io.IOException error) {
                throw new AssertionError(error);
            }
        }));
        try {
            for (int id = 1; id <= 4; id++) {
                loader.handleUploadEvent(event(id, 3, 1));
                assertTrue(loader.handleUploadChunk(id, 0, new byte[]{1, 2, 3})
                    .get("complete").booleanValue());
                await(() -> registry.getActiveCount() == 0);
            }
            assertEquals(0, registry.getReservedBytes());
            assertEquals(2, registry.getReceiptCount());
            assertEquals(0, this.files());
            assertThrows(java.io.IOException.class, () -> stream.get().read());
            assertTrue(loader.handleUploadChunk(4, 0, new byte[]{1, 2, 3})
                .get("complete").booleanValue());
            assertFalse(loader.handleUploadChunk(4, 0, new byte[]{9, 2, 3})
                .get("result").booleanValue());
            assertEquals(4, calls.get());
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(5001));
            registry.cleanup();
            assertEquals(0, registry.getReceiptCount());
            assertFalse(loader.handleUploadChunk(4, 0, new byte[]{1, 2, 3})
                .get("result").booleanValue());
            loader.handleUploadEvent(event(4, 3, 1));
            assertEquals(0, registry.getActiveCount());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Selected bytes count before data arrives; client and application quotas are independent.
     */
    @Test
    public void enforcesAggregateReservationsAcrossClients() throws Exception {
        final Options options = this.options(2, 3, 12, 4);
        final UploadRegistry registry = new UploadRegistry(options);
        final RootWidget first = new RootWidget(options, registry);
        final RootWidget second = new RootWidget(options, registry);
        final FileLoader a = new FileLoader();
        final FileLoader b = new FileLoader();
        first.add(new Section(a));
        second.add(new Section(b));
        final List<UploadingFile> selected = new ArrayList<>();
        a.onSelect(selected::add);
        b.onSelect(selected::add);
        try {
            a.handleUploadEvent(event(1, 4, 1));
            a.handleUploadEvent(event(2, 4, 1));
            a.handleUploadEvent(event(3, 0, 1));
            b.handleUploadEvent(event(1, 8, 2));
            assertEquals(2, registry.getActiveCount());
            b.handleUploadEvent(event(2, 4, 1));
            b.handleUploadEvent(event(3, 0, 1));
            assertEquals(3, selected.size());
            assertEquals(12, registry.getReservedBytes());
            selected.get(0).cancel();
            b.handleUploadEvent(event(4, 4, 1));
            assertEquals(3, registry.getActiveCount());
            assertEquals(12, registry.getReservedBytes());
        } finally {
            first.closeUploads();
            second.closeUploads();
        }
        assertEquals(0, registry.getReservedBytes());
    }

    /**
     * Cancellation, malformed chunks, detachment and absolute TTL all delete partial files.
     */
    @Test
    public void cleansPartialFilesForEveryTerminalPath() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            final AtomicLong clock = new AtomicLong();
            final Options options = this.options(2, 4, 100, 4);
            final UploadRegistry registry = new UploadRegistry(options, clock::get);
            final RootWidget root = new RootWidget(options, registry);
            final FileLoader loader = new FileLoader();
            root.add(new Section(loader));
            final AtomicReference<UploadingFile> selected = new AtomicReference<>();
            loader.onSelect(selected::set);
            try {
                loader.handleUploadEvent(event(1, 8, 2));
                assertTrue(loader.handleUploadChunk(1, 0, new byte[4])
                    .get("result").booleanValue());
                assertEquals(1, this.files());
                switch (mode) {
                    case 0 -> selected.get().cancel();
                    case 1 -> loader.handleUploadChunk(1, 1, new byte[3]);
                    case 2 -> loader.remove();
                    case 3 -> clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(10001));
                    default -> throw new AssertionError(mode);
                }
                registry.cleanup();
                assertEquals(0, registry.getActiveCount());
                assertEquals(0, this.files());
            } finally {
                root.closeUploads();
            }
        }
    }

    /**
     * Application-owned registry shutdown deletes partial files and rejects further data.
     */
    @Test
    public void registryShutdownCancelsUploadsAndReleasesFiles() throws Exception {
        for (int iteration = 0; iteration < 3; iteration++) {
            final Options options = this.options(2, 4, 100, 4);
            final UploadRegistry registry = new UploadRegistry(options);
            final RootWidget root = new RootWidget(options, registry);
            final FileLoader loader = new FileLoader();
            root.add(new Section(loader));
            loader.handleUploadEvent(event(1, 8, 2));
            assertTrue(loader.handleUploadChunk(1, 0, new byte[4]).get("result").booleanValue());
            assertEquals(1, this.files());

            registry.close();
            registry.close();

            assertEquals(0, this.files());
            assertEquals(0, registry.getActiveCount());
            assertEquals(0, registry.getReservedBytes());
            loader.handleUploadEvent(event(2, 8, 2));
            assertFalse(loader.handleUploadChunk(2, 0, new byte[4]).get("result").booleanValue());
            root.closePage();
        }
    }

    /**
     * Controller exceptions cannot retain reservations or make final retries repeat onLoad.
     */
    @Test
    public void releasesOnSelectionAndCompletionErrors() throws Exception {
        final Options options = this.options(2, 4, 100, 4);
        final RootWidget root = new RootWidget(options);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final UploadRegistry registry = root.getUploadRegistry();
        final AtomicInteger calls = new AtomicInteger();
        try {
            loader.onSelect(file -> {
                throw new IllegalArgumentException("Application failure");
            });
            assertThrows(IllegalArgumentException.class,
                () -> loader.handleUploadEvent(event(1, 3, 1)));
            assertEquals(0, registry.getActiveCount());
            loader.onSelect(descriptor -> descriptor.onLoad(file -> {
                calls.incrementAndGet();
                throw new IllegalStateException("Completion failure");
            }));
            loader.handleUploadEvent(event(2, 3, 1));
            loader.handleUploadChunk(2, 0, new byte[3]);
            await(() -> registry.getActiveCount() == 0);
            assertEquals(0, this.files());
            assertTrue(loader.handleUploadChunk(2, 0, new byte[3]).get("complete").booleanValue());
            assertEquals(1, calls.get());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * A retained copy survives callback return while the framework-owned path is deleted.
     */
    @Test
    public void supportsStreamingAndApplicationOwnedCopies() throws Exception {
        final Options options = this.options(2, 4, 100, 4);
        final RootWidget root = new RootWidget(options);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final Path saved = this.directory.getRoot().toPath().resolve("saved.bin");
        loader.onSelect(descriptor -> descriptor.onLoad(file -> {
            try (InputStream input = file.openStream()) {
                assertEquals(3, input.readAllBytes().length);
                file.copyTo(saved);
            } catch (final java.io.IOException error) {
                throw new AssertionError(error);
            }
        }));
        try {
            loader.handleUploadEvent(event(1, 3, 1));
            loader.handleUploadChunk(1, 0, new byte[]{4, 5, 6});
            await(() -> root.getUploadRegistry().getActiveCount() == 0);
            assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(saved));
            assertEquals(1, this.files());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Concurrent clients contend for one atomic server budget, without over-admission.
     */
    @Test
    public void concurrentClientsReachTheSameGlobalLimit() throws Exception {
        final Options options = this.options(2, 3, 12, 4);
        final UploadRegistry registry = new UploadRegistry(options);
        final List<RootWidget> roots = new ArrayList<>();
        final List<java.util.concurrent.Callable<Void>> jobs = new ArrayList<>();
        final AtomicInteger admitted = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);
        final var workers = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            for (int index = 0; index < 8; index++) {
                final RootWidget root = new RootWidget(options, registry);
                final FileLoader loader = new FileLoader();
                roots.add(root);
                root.add(new Section(loader));
                loader.onSelect(file -> admitted.incrementAndGet());
                jobs.add(() -> {
                    start.await();
                    loader.handleUploadEvent(event(1, 4, 1));
                    return null;
                });
            }
            final var futures = jobs.stream().map(workers::submit).toList();
            start.countDown();
            for (final var future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            assertEquals(3, admitted.get());
            assertEquals(3, registry.getActiveCount());
            assertEquals(12, registry.getReservedBytes());
        } finally {
            start.countDown();
            workers.shutdownNow();
            roots.forEach(RootWidget::closeUploads);
        }
        assertEquals(0, registry.getActiveCount());
    }

    /**
     * Processing work remains charged; disposal cancels queued callbacks and closes files.
     */
    @Test
    public void boundsCallbackWorkAndCancelsItOnDisposal() throws Exception {
        final Options options = this.options(6, 6, 24, 8);
        final RootWidget root = new RootWidget(options);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final CountDownLatch entered = new CountDownLatch(4);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        loader.onSelect(descriptor -> descriptor.onLoad(file -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                release.await();
            } catch (final InterruptedException expected) {
                Thread.currentThread().interrupt();
            }
        }));
        try {
            for (int id = 1; id <= 6; id++) {
                loader.handleUploadEvent(event(id, 4, 1));
                loader.handleUploadChunk(id, 0, new byte[4]);
            }
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertEquals(6, root.getUploadRegistry().getActiveCount());
            assertEquals(24, root.getUploadRegistry().getReservedBytes());
            assertEquals(6, this.files());
            root.closeUploads();
            await(() -> root.getUploadRegistry().getActiveCount() == 0);
            assertEquals(0, this.files());
            assertEquals(0, root.getUploadRegistry().getReceiptCount());
            assertEquals(4, calls.get());
        } finally {
            root.closeUploads();
            release.countDown();
        }
    }

    /**
     * A full processing queue rejects further final chunks and discards their storage.
     */
    @Test
    public void configuredQueueAppliesBackpressureAcrossManyFiles() throws Exception {
        final Options options = new Options.Builder().setChunkSize(4).setUploadProcessing(1, 1)
            .setUploadLimits(this.options(16, 16, 100, 16).getUploadLimits()).build();
        final RootWidget root = new RootWidget(options);
        final var registry = root.getUploadRegistry();
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();
        final AtomicInteger calls = new AtomicInteger();
        loader.onSelect(descriptor -> descriptor.onLoad(file -> {
            calls.incrementAndGet();
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException expected) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
            }
        }));
        try {
            loader.handleUploadEvent(event(1, 4, 1));
            assertTrue(loader.handleUploadChunk(1, 0, new byte[4]).get("result").booleanValue());
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            loader.handleUploadEvent(event(2, 4, 1));
            assertTrue(loader.handleUploadChunk(2, 0, new byte[4]).get("result").booleanValue());
            for (int id = 3; id <= 12; id++) {
                loader.handleUploadEvent(event(id, 4, 1));
                final var busy = loader.handleUploadChunk(id, 0, new byte[4]);
                assertFalse(busy.get("result").booleanValue());
                assertEquals("processingBusy", busy.get("uploadError").stringValue());
            }
            assertEquals(1, calls.get());
            assertEquals(2, registry.getActiveCount());
            assertEquals(2, this.files());
            release.countDown();
            await(() -> registry.getActiveCount() == 0);
            assertEquals(2, calls.get());
            assertEquals(1, peak.get());
            assertEquals(0, this.files());
        } finally {
            release.countDown();
            root.closePage();
            registry.close();
        }
    }

    /**
     * Processing results enter the root after progress and cannot update a closed page.
     */
    @Test
    public void resultHandoffIsSerializedWithProgressAndClose() throws Exception {
        final RootWidget root = new RootWidget(this.options(2, 4, 100, 4));
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        final var log = new ArrayList<String>();
        final CountDownLatch result = new CountDownLatch(1);
        final var selected = new AtomicReference<UploadingFile>();
        final Listener<Integer> progress = count -> log.add("progress " + count);
        loader.onSelect(descriptor -> {
            selected.set(descriptor);
            descriptor.getLoadingPercentageModel().addListener(progress);
            descriptor.onLoad(file -> {
                descriptor.executeOnRoot(() -> {
                    assertTrue(Thread.holdsLock(root));
                    log.add("result");
                });
                result.countDown();
            });
        });
        try {
            assertTrue(root.execute(() -> {
                loader.handleUploadEvent(event(1, 8, 2));
                loader.handleUploadChunk(1, 0, new byte[4]);
                loader.handleUploadChunk(1, 1, new byte[4]);
                assertEquals(List.of("progress 50", "progress 100"), log);
            }));
            assertTrue(result.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("progress 50", "progress 100", "result"), log);
            root.closePage();
            assertFalse(root.execute(() -> log.add("late root")));
            assertFalse(selected.get().executeOnRoot(() -> log.add("late file")));
        } finally {
            root.closePage();
            root.getUploadRegistry().close();
            java.lang.ref.Reference.reachabilityFence(progress);
        }
    }

    /**
     * A disconnected browser needs no further request to trigger file cleanup.
     */
    @Test
    public void sweepsAbandonedPartialFilesWithoutPolling() throws Exception {
        final Options options = new Options.Builder().setChunkSize(4)
            .setUploadLimits(new UploadLimits(2, 4, 100, 50, 4, 50,
                this.directory.getRoot().toPath())).build();
        final RootWidget root = new RootWidget(options);
        final FileLoader loader = new FileLoader();
        root.add(new Section(loader));
        try {
            loader.handleUploadEvent(event(1, 8, 2));
            loader.handleUploadChunk(1, 0, new byte[4]);
            await(() -> root.getUploadRegistry().getActiveCount() == 0);
            assertEquals(0, this.files());
        } finally {
            root.closeUploads();
        }
    }

    /**
     * Small deterministic quotas and TTLs avoid allocating large fixtures or relying on GC.
     */
    private Options options(final int client, final int global, final long bytes,
            final int receipts) {
        return new Options.Builder().setChunkSize(4).setMaxFileSize(100)
            .setUploadLimits(new UploadLimits(client, global, bytes, 10000, receipts, 5000,
                this.directory.getRoot().toPath())).build();
    }

    /**
     * Builds a structurally valid descriptor for direct Java boundary tests.
     */
    private static UploadEvent event(final int id, final int size, final int chunks) {
        final UploadEvent event = new UploadEvent();
        event.fileId = id;
        event.name = "data.bin";
        event.type = "application/octet-stream";
        event.size = size;
        event.totalChunks = chunks;
        return event;
    }

    /**
     * @return all files left in the dedicated temporary storage directory
     */
    private long files() throws Exception {
        try (var files = Files.list(this.directory.getRoot().toPath())) {
            return files.count();
        }
    }

    /**
     * @return whether the completed file has lost access to its framework-owned content
     */
    private static boolean closed(final UploadedFile file) {
        try {
            file.getContent();
            return false;
        } catch (final IllegalStateException expected) {
            return true;
        }
    }

    /**
     * Waits for asynchronous resource cleanup with a strict failure deadline.
     */
    private static void await(final BooleanSupplier condition) throws Exception {
        final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < end) {
            Thread.sleep(5);
        }
        assertTrue("Resource cleanup did not finish", condition.getAsBoolean());
    }
}
