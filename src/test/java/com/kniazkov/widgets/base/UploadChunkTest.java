/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.webserver.FileDescriptor;
import com.kniazkov.webserver.Request;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests multipart upload routing, ownership checks, and configured size limits. */
public class UploadChunkTest {
    /** A valid binary part is delivered only to the file loader owned by its client. */
    @Test
    public void routesBinaryChunkToOwnedFileLoader() {
        final Fixture fixture = new Fixture(16);

        assertTrue(result(fixture.handler.process(fixture.request(new byte[] {0, -1, 7, 12}, 4))));
        assertFalse(result(fixture.handler.process(
            fixture.requestForClient(RMId.create(), new byte[] {1}, 1)
        )));
    }

    /** The configured total-file limit is checked before upload state is allocated. */
    @Test
    public void rejectsFileAboveConfiguredLimit() {
        final Fixture fixture = new Fixture(3);

        assertFalse(result(fixture.handler.process(fixture.request(new byte[] {1, 2, 3, 4}, 4))));
    }

    /** Missing file parts and malformed integers return an explicit negative acknowledgement. */
    @Test
    public void rejectsMalformedMultipartRequest() {
        final Fixture fixture = new Fixture(16);
        final Request missingFile = fixture.request(new byte[] {1}, 1);
        missingFile.files.clear();
        final Request malformedSize = fixture.request(new byte[] {1}, 1);
        malformedSize.formData.put("size", "not-a-number");

        assertFalse(result(fixture.handler.process(missingFile)));
        assertFalse(result(fixture.handler.process(malformedSize)));
    }

    private static boolean result(final JsonElement response) {
        return response.toJsonObject().get("result").getBooleanValue();
    }

    /** Fully initialized client/widget fixture for the package-private action handler. */
    private static final class Fixture {
        private final UploadChunk handler;
        private final RMId clientId;
        private final FileLoader loader;

        Fixture(final int maxUploadSize) {
            final AtomicReference<FileLoader> created = new AtomicReference<>();
            final Page page = (root, context) -> {
                final FileLoader value = new FileLoader("Upload");
                created.set(value);
                root.add(new Section(value));
            };
            final Options options = new Options();
            options.maxUploadSize = maxUploadSize;
            final Application application = new Application(page);
            application.setOptions(options);
            this.clientId = application.createClient("/", new PageContext());
            this.loader = created.get();
            this.handler = new UploadChunk(application);
        }

        Request request(final byte[] content, final int size) {
            return this.requestForClient(this.clientId, content, size);
        }

        Request requestForClient(final RMId client, final byte[] content, final int size) {
            final Request request = new Request();
            request.formData.put("client", client.toString());
            request.formData.put("widget", this.loader.getId().toString());
            request.formData.put("fileId", "1");
            request.formData.put("name", "file.bin");
            request.formData.put("type", "application/octet-stream");
            request.formData.put("size", Integer.toString(size));
            request.formData.put("chunkIndex", "0");
            request.formData.put("totalChunks", "1");
            final FileDescriptor file = new FileDescriptor();
            file.name = "chunk.bin";
            file.contentType = "application/octet-stream";
            file.data = content;
            request.files.put("file", file);
            return request;
        }
    }
}
