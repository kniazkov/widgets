/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests the request handlers that form the base package protocol boundary. */
public class BaseHandlersTest {
    /** Verifies creation context, reserved parameters, and client destruction. */
    @Test
    public void createAndKillHandlersCompleteAClientLifecycle() {
        final AtomicReference<PageContext> captured = new AtomicReference<>();
        final Application application = BaseTestSupport.application(
            (root, context) -> captured.set(context)
        );
        final UUID browserId = UUID.randomUUID();
        final Map<String, String> request = new TreeMap<>();
        request.put("action", "new instance");
        request.put("address", "/");
        request.put("browserId", browserId.toString());
        request.put("mobile", "true");
        request.put("item", "42");

        final JsonObject response = new CreateClient(application).process(request).toJsonObject();
        final UId clientId = UId.parse(response.get("id").getStringValue());

        assertTrue(clientId.isValid());
        assertNotNull(captured.get());
        assertEquals(browserId, captured.get().browserId);
        assertTrue(captured.get().mobile);
        assertEquals(Collections.singletonMap("item", "42"), captured.get().parameters);

        final Map<String, String> killRequest = Collections.singletonMap(
            "client", clientId.toString()
        );
        assertTrue(new KillClient(application).process(killRequest).getBooleanValue());
        assertFalse(new KillClient(application).process(killRequest).getBooleanValue());
    }

    /** A successful synchronization must be distinguishable from a missing client. */
    @Test
    public void synchronizeReportsThatTheClientExists() {
        final Application application = BaseTestSupport.application((root, context) -> { });
        final UId clientId = application.createClient("/", new PageContext());
        final Map<String, String> request = Collections.singletonMap(
            "client", clientId.toString()
        );

        final JsonElement response = new Synchronize(application).process(request);

        assertTrue(
            "A live client was synchronized, but the protocol still returned result=false",
            response.toJsonObject().get("result").getBooleanValue()
        );
    }
}
