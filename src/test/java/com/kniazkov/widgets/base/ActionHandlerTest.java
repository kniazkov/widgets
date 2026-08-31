/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the client protocol failure boundary.
 */
public final class ActionHandlerTest {
    /**
     * Application failures become an explicit fatal response for the browser.
     */
    @Test
    public void reportsClientFailure() {
        final Application application = BaseTestSupport.application(
            (root, context) -> { }
        );
        final ActionHandler handler = new ActionHandler(application) {
            @Override
            JsonElement process(final Map<String, String> data) {
                throw new IllegalStateException("client failed");
            }
        };

        final JsonObject response = handler.processSafely(
            Collections.emptyMap()
        ).toJsonObject();

        assertFalse(response.get("result").getBooleanValue());
        assertTrue(response.get("clientError").getBooleanValue());
    }
}
